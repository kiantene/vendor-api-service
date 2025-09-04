package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientApiRequest;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.common.OperatorApiCallerV2;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.entity.warehouse.PromoPayoutHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoPayoutProcessor implements CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final PromoPayoutMapper mapper;
    private final OperatorApiCallerV2 operatorApiCaller;
    private final ClientRequestService clientRequestService;
    private final KafkaService kafkaService;
    private final BetResultRetryLogService betResultRetryLogService;

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        context.setTransactionId(UuidUtil.newUuidV7StringRaw());

        // TODO : currency conversion
        // TODO : store in couchbase?

        preProcess(context);
        return callToOperator(context);
    }

    private void preProcess(PromoPayoutContext context) {
        PromoPayoutHistory promoPayoutHistory = PromoPayoutHistory.builder()
                .transactionId(context.getTransactionId())
                .vendorTransactionId(context.getVendorTransactionId())
                .campaignUuid(context.getCampaignUuid())
                .agentPlayerId(context.getAgentPlayerId())
                .agentPlayerUsername(context.getAgentPlayerUsername())
                .vendorPlayerId(context.getVendorPlayerId())
                .vendorPlayerUsername(context.getVendorPlayerUsername())


                .vendorId(context.getVendorId())
                .vendorCode(context.getVendorCode())
                .vendorLineId(context.getVendorLineId())

                .agentId(context.getAgentId())
                .masterAgentId(context.getMasterAgentId())
                .houseId(context.getHouseId())

                .currencyId(context.getCurrencyId())
                .currencyCode(context.getCurrencyCode())
                .payoutAmount(context.getPayoutAmount())
                .promoType(PromoType.FREE_ROUND.id)
                .status(BetStatus.SETTLED.code)
                .vendorTransactionTime(context.getVendorTransactionTime())
                .build();
        kafkaService.producePromoPayoutHistory(promoPayoutHistory);
    }

    private PlayerBalanceData callToOperator(PromoPayoutContext context) {
        try {
            ClientApiRequest<PromoPayoutRequest> apiRequest = clientRequestService.createClientApiRequest(
                    context.getAgentId(),
                    EndPoints.PROMO_PAYOUT,
                    mapper.toPromoPayoutRequest(context)
            );
            ClientBalanceResponse response;
            if (clientRequestService.shouldMockResponse(context.getAgentPlayerUsername())) {
                response = clientRequestService.mockClientResponse(
                        context.getTraceId(),
                        context.getCurrencyCode(),
                        context.getAgentPlayerUsername()
                );
            } else {
                response = operatorApiCaller.post(
                        apiRequest.getBaseUrl(),
                        apiRequest.getPath(),
                        apiRequest.getHeaders(),
                        apiRequest.getRequestObject()
                );
            }
//            processor.onSuccess(context, response);

            return response.getData();
        } catch (Exception ex) {
//            processor.onError(context, clientRequestAuth, ex);
            throw ex;
        }
    }

    @Override
    public void onSuccess(PromoPayoutContext context, ClientBalanceResponse result) {
        // TODO : currency conversion
    }

    @Override
    public void onError(PromoPayoutContext context, ClientApiRequest<?> clientApiRequest, Exception ex) {
        try {
            String operatorData = objectMapper.writeValueAsString(clientApiRequest.getRequestObject());
            betResultRetryLogService.create(operatorData,
                    context.getVendorId(),
                    context.getAgentId(),
                    context.getVendorTransactionId(),
                    context.getVendorTransactionId(),
                    context.getTransactionId(),
                    clientApiRequest.getPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
