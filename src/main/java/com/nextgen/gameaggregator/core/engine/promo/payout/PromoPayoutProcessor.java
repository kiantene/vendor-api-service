package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.util.UuidUtil;
import com.nextgen.gameaggregator.entity.warehouse.PromoPayoutHistory;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PromoPayoutProcessor implements CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> {
    private final KafkaService kafkaService;

    @Override
    public void process(PromoPayoutContext context) {
        context.setTransactionId(UuidUtil.newUuidV7StringRaw());
    }

    @Override
    public void onSuccess(PromoPayoutContext context, ClientBalanceResponse result) {
        PromoPayoutHistory promoPayoutHistory = PromoPayoutHistory.builder()
                .id(UuidUtil.newUuidV7StringRaw())
                .externalTransactionId(context.getTransactionId())
                .vendorBetId(context.getTransactionId())
                .roundId(context.getTransactionId())
                .vendorGameId(1) // TODO : replace
                .gameCode("gameCode")
                .vendorPlayerId(context.getVendorPlayerId())
                .vendorPlayerUsername(context.getVendorPlayerUsername())
                .vendorId(2) // TODO : replace
                .vendorCode("PGS") // TODO : replace
                .vendorLineId(2) // TODO : replace
                .agentPlayerId(context.getAgentPlayerId())
                .agentPlayerUsername(context.getAgentPlayerUsername())
                .agentId(context.getAgentId())
                .operatorStatus(1) // TODO : replace
                .gameCategoryId(1) // TODO : replace
                .gameCategoryCode("SLOT") // TODO : replace
                .currencyId(1) // TODO : replace
                .currencyCode("USD") // TODO : replace
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
    public void onError(PromoPayoutContext context, Exception ex) {

    }
}
