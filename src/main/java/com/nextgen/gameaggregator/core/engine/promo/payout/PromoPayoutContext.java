package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.context.VendorGameAware;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.enums.PromoType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PromoPayoutContext implements VendorPlayerAware, VendorGameAware {
    // Request
    private String traceId;
    private String idempotencyKey;

    // Promo
    private String type;
    private String vendorClassName;
    private String campaignUuid;
    private PromoType promoType;

    // GA
    private Integer agentId;
    private Integer masterAgentId;
    private Integer houseId;
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
    private String gameName;
    private Integer gameCategoryId;
    private String gameCategoryCode;

    // Vendor Class
    private String transactionId;
    private String vendorTransactionId;
    private BigDecimal payoutAmount;
    private Long vendorTransactionTime;

}
