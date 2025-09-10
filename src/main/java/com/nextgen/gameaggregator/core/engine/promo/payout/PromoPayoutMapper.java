package com.nextgen.gameaggregator.core.engine.promo.payout;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PromoPayoutMapper {
    public PromoPayoutRequest toPromoPayoutRequest(PromoPayoutContext context) {
        if (context == null) {
            return null;
        }

        return PromoPayoutRequest.builder()
                .traceId(context.getTraceId())
                .username(context.getAgent().playerUsername())
                .transactionId(context.getTransactionId())
                .campaignId(Objects.requireNonNullElse(context.getCampaignUuid(), null))
                .currency(context.getCurrencyCode())
                .amount(context.getPayoutAmount())
                .type(context.getPromoType().code)
                .timestamp(context.getVendorTransactionTime())
                .build();
    }
}
