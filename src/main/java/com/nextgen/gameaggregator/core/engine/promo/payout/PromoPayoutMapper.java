package com.nextgen.gameaggregator.core.engine.promo.payout;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PromoPayoutMapper {
    public PromoPayoutDto toPromoPayoutRequest(PromoPayoutContext context) {
        if (context == null) {
            return null;
        }

        return PromoPayoutDto.builder()
                .traceId(context.getTraceId())
                .username(context.getAgent().playerUsername())
                .transactionId(context.getTransactionId())
                .campaignId(context.getCampaignUuid())
                .currency(context.getCurrencyCode())
                .amount(context.getPayout().amount())
                .timestamp(context.getVendorTransactionTime())
                .build();
    }

    public PromoPayoutDto toPromoPayoutRequest(PromoPayoutContext context, PayoutTransaction txn) {
        if (context == null) {
            return null;
        }

        return PromoPayoutDto.builder()
                .traceId(txn.getTraceId())
                .username(context.getAgent().playerUsername()) // TODO: potential issue if txn username != context username
                .transactionId(txn.getTransactionId())
                .campaignId(context.getCampaignUuid())
                .currency(context.getCurrencyCode())
                .amount(txn.getPayout().amount())
                .timestamp(txn.getVendorTransactionTime())
                .build();
    }
}
