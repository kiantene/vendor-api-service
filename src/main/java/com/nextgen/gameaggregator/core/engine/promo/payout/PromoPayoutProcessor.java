package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.core.api.ApiResult;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LoggingManager;
import com.nextgen.gameaggregator.core.retry.RetryHelper;
import com.nextgen.gameaggregator.core.retry.RetryPolicy;
import com.nextgen.gameaggregator.core.retry.RetryQueueService;
import com.nextgen.gameaggregator.core.retry.enums.RetryOrigin;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorReactiveApiAdapter;
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
public class PromoPayoutProcessor {
    private final PromoPayoutMapper mapper;
    private final ClientRequestService clientRequestService;
    private final OperatorApiAdapter operatorApiAdapter;
    private final OperatorReactiveApiAdapter reactiveApiAdapter;
    private final PromoPayoutHistoryProducer producer;
    private final RetryQueueService retryQueueService;

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
                .filter(this::hasPayout) // only process for transactions that has payout > 0
                .flatMap(tx -> {
                    tx.setTraceId(context.getTraceId()); // use same traceId so that we can group all payouts together
                    tx.setTransactionId(UuidUtil.newUuidV7StringRaw()); // operator should use transactionId for idempotency
                    producer.publish(context, tx);

                    LogContext logContext = LogContextHolder.get().copy(); // create copy for this transaction

                    return callToOperatorAsync(context, tx, logContext)
                            .doOnNext(balanceData -> LoggingManager.printLog(logContext))
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

    private boolean hasPayout(PayoutTransaction tx) {
        return tx.getVendorPayoutAmount() != null && tx.getVendorPayoutAmount().signum() > 0;
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
        OperatorApiRequest apiRequest = toApiRequest(context, requestDto);

        try {
            ApiResult apiResult = operatorApiAdapter.execute(apiRequest);
            apiResult.throwIfError();
            ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

            if (response.getData() != null) {
                return response.getData();
            }
            return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), context.getVendorCurrency());

        } catch (Exception ex) {
            if (RetryPolicy.shouldRetry(RetryOrigin.PROMO_PAYOUT, ex)) {
                retryQueueService
                        .enqueue(RetryHelper.toHttpCallSpec(apiRequest), RetryOrigin.PROMO_PAYOUT)
                        .subscribe();
            }
        }

        return PlayerBalanceData.getDefault(context.getVendorPlayerUsername(), context.getVendorCurrency());
    }

    private Mono<PlayerBalanceData> callToOperatorAsync(PromoPayoutContext context, PayoutTransaction txn, LogContext logContext) {
        PromoPayoutDto requestDto = mapper.toPromoPayoutRequest(context, txn);

        OperatorApiRequest apiRequest = toApiRequest(context, requestDto);

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

        return reactiveApiAdapter.execute(apiRequest, logContext)
                .map(apiResult -> {
                    apiResult.throwIfError();
                    ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

                    return response.getData();
                })
                // retry handling: enqueue failed request
                .onErrorResume(handleErrorWithRetry);
    }

    private OperatorApiRequest toApiRequest(PromoPayoutContext context, PromoPayoutDto dto) {
        return clientRequestService.createOperatorApiRequest(
                context.getTraceId(),
                context.getAgent().id(),
                context.getAgent().playerUsername(),
                EndPoints.PROMO_PAYOUT,
                dto,
                dto.getTimestamp()
        );
    }
}
