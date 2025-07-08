package com.nextgen.gameaggregator.core.engine.promo.payout;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PromoPayoutContext {

    private String idempotencyKey;

    // GA
    private Long agentPlayerId;
    private Long vendorPlayerId;
    private String agentPlayerUsername;
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

    // Vendor Class
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long resultTime;

}
