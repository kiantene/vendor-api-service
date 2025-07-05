package com.nextgen.gameaggregator.core.engine.promo.payout;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PromoPayoutContext {

    private String idempotencyKey;

    private String vendorPlayerUsername;
    private String vendorCurrency;
    private BigDecimal amount;
    private Long timestamp;

    private String traceId;
    private String transactionId;
    private String currency;
    private String type;
    private String vendorClassName;
    private Integer agentId;
}
