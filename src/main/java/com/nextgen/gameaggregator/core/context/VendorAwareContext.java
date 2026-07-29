package com.nextgen.gameaggregator.core.context;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public abstract class VendorAwareContext extends VendorRequestContext implements VendorPlayerAware, VendorGameAware, VendorCurrencyAware {

    /**
     * populated by {@link com.nextgen.gameaggregator.core.context.BaseEnricher} via VendorPlayerAware
     */
    private Integer vendorId;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private Long vendorPlayerId;
    private Integer currencyId;
    private Integer vendorLineId;

    /**
     * populated by {@link com.nextgen.gameaggregator.core.context.BaseEnricher} via VendorGameAware
     */
    private Integer vendorGameId;
    private String gameCode;
    private String gameName;
    private Integer gameCategoryId;

    /**
     * populated by {@link com.nextgen.gameaggregator.core.context.BaseEnricher} via VendorCurrencyAware
     */
    private String currencyCode;
    private BigDecimal fromVendorRate;
    private BigDecimal toVendorRate;

}
