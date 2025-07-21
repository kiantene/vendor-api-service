package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.common.ClientRequestAuth;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.util.UuidUtil;
import com.nextgen.gameaggregator.entity.warehouse.PromoPayoutHistory;
import com.nextgen.gameaggregator.service.BetResultRetryLogService;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
                .id(context.getTransactionId())
                .externalTransactionId(context.getExternalTransactionId())
                .vendorGameId(context.getVendorGameId())
                .gameCode(context.getGameCode())
                .vendorPlayerId(context.getVendorPlayerId())
                .vendorPlayerUsername(context.getVendorPlayerUsername())
                .vendorId(context.getVendorId())
//                .vendorCode(context.getVendorCode())
                .vendorLineId(context.getVendorLineId())
                .agentPlayerId(context.getAgentPlayerId())
                .agentPlayerUsername(context.getAgentPlayerUsername())
                .agentId(context.getAgentId())
//                .gameCategoryId(context.getGameCategoryId())
//                .gameCategoryCode(context.getGameCategoryCode())
                .currencyId(context.getCurrencyId())
                .currencyCode(context.getCurrency())
                .betAmount(context.getBetAmount())
                .winAmount(context.getWinAmount())
                .winLoss(context.getWinLoss())
                .effectiveTurnover(context.getEffectiveTurnover())
                .jackpotAmount(BigDecimal.ZERO) // TODO : replace
                .resultType(1) // TODO : replace
                .isFreespin(0) // TODO : replace
                .status(1) // TODO : replace
                .vendorBetTime(context.getVendorBetTime())
                .vendorSettleTime(context.getVendorSettleTime())
                .resultTime(context.getResultTime())
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
                    context.getExternalTransactionId(),
                    context.getExternalTransactionId(),
                    context.getTransactionId(),
                    clientRequestAuth.getPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
