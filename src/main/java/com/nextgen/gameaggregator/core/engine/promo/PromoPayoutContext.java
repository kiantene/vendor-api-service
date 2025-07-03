package com.nextgen.gameaggregator.core.engine.promo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromoPayoutContext {

    private String idempotencyKey;

    private String vendorPlayerUsername;
    private String vendorCurrency;
    private BigDecimal amount;
    private Long timestamp;

    private String traceId;
    private String vendorClassName;
}
