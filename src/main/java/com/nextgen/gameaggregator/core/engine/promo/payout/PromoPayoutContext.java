package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PromoPayoutContext implements VendorPlayerAware {
    // Request
    private String traceId;
    private String idempotencyKey;

    // Promo
    private String type;
    private String vendorClassName;

    // GA
    private Integer agentId;
    private Integer vendorId;
    private String vendorCode;
    private Integer vendorLineId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private Long vendorPlayerId;
    private String vendorPlayerUsername;
    private String vendorCurrency;
    private Integer currencyId;
    private String currency;
    private String vendorGameCode;
    private Integer vendorGameId;
    private String gameCode;
    private Integer gameCategoryId;
    private String gameCategoryCode;

    // Vendor Class
    private String transactionId;
    private String externalTransactionId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long resultTime;

}
