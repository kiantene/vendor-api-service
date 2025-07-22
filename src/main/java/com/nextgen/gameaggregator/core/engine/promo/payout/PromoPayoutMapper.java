package com.nextgen.gameaggregator.core.engine.promo.payout;

import org.springframework.stereotype.Component;

@Component
public class PromoPayoutMapper {
    public PromoPayoutRequest toPromoPayoutRequest(PromoPayoutContext context) {
        if (context == null) {
            return null;
        }

        return PromoPayoutRequest.builder()
                .traceId(context.getTraceId())
                .username(context.getAgentPlayerUsername())
                .transactionId(context.getTransactionId())
                .currency(context.getCurrency())
                .amount(context.getPayoutAmount())
                .type(context.getPromoType().code)
                .timestamp(context.getVendorTransactionTime())
                .build();
    }
}
