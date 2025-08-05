package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.util.UuidUtil;
import com.nextgen.gameaggregator.core.common.ClientRequestAuth;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.entity.warehouse.PromoPayoutHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.PromoType;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoPayoutProcessor implements CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> {
    private final KafkaService kafkaService;
    private final ObjectMapper objectMapper;
    private final BetResultRetryLogService betResultRetryLogService;

    @Override
    public void process(PromoPayoutContext context) {
        context.setTransactionId(UuidUtil.newUuidV7StringRaw());

        // TODO : currency conversion
        // TODO : store in couchbase?

        PromoPayoutHistory promoPayoutHistory = PromoPayoutHistory.builder()
                .transactionId(context.getTransactionId())
                .vendorTransactionId(context.getVendorTransactionId())
                .campaignUuid(context.getCampaignUuid())
                .agentPlayerId(context.getAgentPlayerId())
                .agentPlayerUsername(context.getAgentPlayerUsername())
                .vendorPlayerId(context.getVendorPlayerId())
                .vendorPlayerUsername(context.getVendorPlayerUsername())

                .vendorGameId(context.getVendorGameId())
                .gameName(context.getGameName())
                .gameCode(context.getGameCode())

                .vendorId(context.getVendorId())
                .vendorCode(context.getVendorCode())
                .vendorLineId(context.getVendorLineId())

                .gameCategoryId(context.getGameCategoryId())
                .gameCategoryCode(context.getGameCategoryCode())
                .agentId(context.getAgentId())
                .masterAgentId(context.getMasterAgentId())
                .houseId(context.getHouseId())

                .currencyId(context.getCurrencyId())
                .currencyCode(context.getCurrency())
                .payoutAmount(context.getPayoutAmount())
                .promoType(PromoType.FREE_ROUND.id)
                .status(BetStatus.SETTLED.code)
                .vendorTransactionTime(context.getVendorTransactionTime())
                .build();
        kafkaService.producePromoPayoutHistory(promoPayoutHistory);

    }

    @Override
    public void onSuccess(PromoPayoutContext context, ClientBalanceResponse result) {
        // TODO : currency conversion

    }

    @Override
    public void onError(PromoPayoutContext context, ClientRequestAuth<?> clientRequestAuth, Exception ex) {
        try {
            String operatorData = objectMapper.writeValueAsString(clientRequestAuth.getRequestObject());
            betResultRetryLogService.create(operatorData,
                    context.getVendorId(),
                    context.getAgentId(),
                    context.getVendorTransactionId(),
                    context.getVendorTransactionId(),
                    context.getTransactionId(),
                    clientRequestAuth.getPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
