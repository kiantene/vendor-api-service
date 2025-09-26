package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.retry.RetryHelper;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import com.nextgen.gameaggregator.core.retry.RetryPolicy;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.webclient.*;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.data.model.TxnAmount;
import com.nextgen.gameaggregator.service.data.producer.PromoPayoutHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoPayoutProcessor implements CoreEngineProcessor<PromoPayoutContext, ClientApiResponse> {
    private final PromoPayoutMapper mapper;
    private final ClientRequestService clientRequestService;
    private final OperatorApiCaller operatorApiCaller;
    private final PromoPayoutHistoryProducer producer;
    private final RetryQueueService retryQueueService;

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {

        producer.publish(context);

        PlayerBalanceData balanceData = callToOperator(context);
        TxnAmount playerBalance = TxnAmount.of(balanceData.getBalance(), context.getToVendorRate());

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                playerBalance.amount(),
                balanceData.getTimestamp()
        );
    }

    public Mono<Void> processBatch(PromoPayoutContext context) {
        final int CONCURRENCY = 16;
        var transactions = context.getPayoutTransactions();

        if (transactions.isEmpty()) return Mono.empty();

        return Flux.fromIterable(transactions)
                .flatMap(tx -> {
                    tx.setTraceId(context.getTraceId()); // use same traceId so that we can group all payouts together
                    tx.setTransactionId(UuidUtil.newUuidV7StringRaw()); // operator should use transactionId for idempotency

                    return callToOperatorAsync(context, tx)
                            .map(balanceData -> {
                                TxnAmount playerBalance = TxnAmount.of(balanceData.getBalance(), context.getToVendorRate());
                                return new PlayerBalanceData(
                                        context.getVendorPlayerUsername(),
                                        context.getVendorCurrency(),
                                        playerBalance.amount(),
                                        balanceData.getTimestamp()
                                );
                            });
                }, CONCURRENCY)
                .then();
    }

    private PlayerBalanceData callToOperator(PromoPayoutContext context) {
        PromoPayoutContext.Agent agent = context.getAgent();

        if (clientRequestService.shouldMockResponse(agent.playerUsername())) {
            return clientRequestService.mockClientResponse(
                    context.getTraceId(),
                    context.getCurrencyCode(),
                    agent.playerUsername()
            ).getData();
        }

        PromoPayoutDto requestDto = mapper.toPromoPayoutRequest(context);
        var apiRequest = clientRequestService.createClientApiRequest(
                context.getTraceId(),
                agent.id(),
                agent.playerUsername(),
                EndPoints.PROMO_PAYOUT,
                requestDto,
                requestDto.getTimestamp()
        );

        try {
            ClientApiResult apiResult = operatorApiCaller.post(apiRequest, DefaultOperatorCallerLifeCycle.get());
            apiResult.throwIfError();
            ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

            return response.getData();
        } catch (Exception ex) {
            if (RetryPolicy.shouldRetry(RetryOrigin.PROMO_PAYOUT, ex)) {
                retryQueueService
                        .enqueue(RetryHelper.toHttpCallSpec(apiRequest), RetryOrigin.PROMO_PAYOUT)
                        .subscribe();
            }
        }

        return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), context.getVendorCurrency());
    }

    private Mono<PlayerBalanceData> callToOperatorAsync(PromoPayoutContext context, PayoutTransaction txn) {
        PromoPayoutContext.Agent agent = context.getAgent();
        PromoPayoutDto requestDto = mapper.toPromoPayoutRequest(context, txn);

        var apiRequest = clientRequestService.createClientApiRequest(
                context.getTraceId(),
                agent.id(),
                agent.playerUsername(),
                EndPoints.PROMO_PAYOUT,
                requestDto,
                requestDto.getTimestamp()
        );

        Function<Throwable, Mono<PlayerBalanceData>> handleErrorWithRetry = ex -> {
            if (RetryPolicy.shouldRetry(RetryOrigin.PROMO_PAYOUT, (Exception) ex)) {
                return retryQueueService.enqueue(RetryHelper.toHttpCallSpec(apiRequest), RetryOrigin.PROMO_PAYOUT)
                        .thenReturn(PlayerBalanceData.getDefault(
                                context.getVendorPlayerUsername(),
                                context.getVendorCurrency()
                        ));
            }
            return Mono.just(PlayerBalanceData.getDefault(
                    context.getVendorPlayerUsername(),
                    context.getVendorCurrency()
            ));
        };

        LogContext logContext = LogContextHolder.get().copy(); // creates a copy from the original logContext

        return operatorApiCaller.postAsync(apiRequest, new DefaultOperatorCallerLifeCycle(logContext))
                .map(apiResult -> {
                    apiResult.throwIfError();
                    ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

                    return response.getData();
                })
                // retry handling: enqueue failed request
                .onErrorResume(handleErrorWithRetry);
    }

    @Override
    public void onSuccess(PromoPayoutContext context, ClientApiResponse result) {
    }

    @Override
    public void onError(PromoPayoutContext context, ClientApiRequest<?> clientApiRequest, Exception ex) {
    }
}
