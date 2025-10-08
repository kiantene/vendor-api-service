package com.nextgen.gameaggregator.service.data.producer;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.entity.warehouse.PromoPayoutHistory;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.service.KafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromoPayoutHistoryProducer {
    private final KafkaService kafkaService;

    public void publish(PromoPayoutContext context) {
        PromoPayoutHistory promoPayoutHistory = buildPromoPayoutHistory(context);

        kafkaService.producePromoPayoutHistory(promoPayoutHistory);
    }

    private PromoPayoutHistory buildPromoPayoutHistory(PromoPayoutContext context) {
        return PromoPayoutHistory.builder()
                .transactionId(context.getTransactionId())
                .vendorTransactionId(context.getVendorTransactionId())
                .campaignUuid(context.getCampaignUuid())
                .agentPlayerId(context.getAgent().playerId())
                .agentPlayerUsername(context.getAgent().playerUsername())
                .vendorPlayerId(context.getVendor().playerId())
                .vendorPlayerUsername(context.getVendorPlayerUsername())

                .vendorId(context.getVendorId())
                .vendorCode(context.getVendor().code())
                .vendorLineId(context.getVendor().lineId())

                .agentId(context.getAgent().id())
                .masterAgentId(context.getAgent().masterAgentId())
                .houseId(context.getAgent().houseId())

                .currencyId(context.getCurrencyId())
                .currencyCode(context.getCurrencyCode())
                .payoutAmount(context.getPayout().amount())
                .promoType(context.getPromoType().id)
                .status(BetStatus.SETTLED.code)
                .vendorTransactionTime(context.getVendorTransactionTime())
                .build();
    }
}
