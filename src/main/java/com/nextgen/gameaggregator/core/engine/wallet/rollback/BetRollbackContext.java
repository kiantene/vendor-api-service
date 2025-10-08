package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.context.VendorCurrencyAware;
import com.nextgen.gameaggregator.core.context.VendorGameAware;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Field mapping rules for Rollback Request:
 * <p></p>
 * <p>idempotencyKey - Unique key to stop duplicate transactions.</p>
 * <p>vendorBetId    - Same as bet request vendorBetId. Used if rollbackType = BY_BET.</p>
 * <p>roundId        - Same as bet request roundId. Used if rollbackType = BY_ROUND.</p>
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class BetRollbackContext extends VendorRequestContext implements
        VendorPlayerAware, VendorGameAware, VendorCurrencyAware {
    /**
     * @deprecated use configure in controller to set rollbackType
     */
    @Deprecated
    private RollbackType rollbackType;
    private String vendorBetId;
    private String roundId;
    private Long timestamp;

    // --- internal values ---
    private BaseVendorService vendorService;
    private GameSession gameSession;
    private HttpRequestLog httpRequestLog;

    /**
     * Indicates whether settled bet data must be fetched from the warehouse
     * before performing the rollback operation.
     */
    private boolean retrieveSettledBet;

    /**
     * populated by {@link com.nextgen.gameaggregator.core.context.BaseEnricher} via VendorPlayerAware
     */
    private Integer vendorId;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private Long vendorPlayerId;
    private Integer vendorLineId;

    /**
     * populated by {@link com.nextgen.gameaggregator.core.context.BaseEnricher} via VendorGameAware
     */
    private Integer vendorGameId;
    private String gameCode;
    private String gameName;
    private Integer gameCategoryId;

    @Builder.Default
    private Currency currency = new Currency();

    @Override
    public Integer getCurrencyId() {
        return this.currency.id();
    }

    @Override
    public void setCurrencyId(Integer currencyId) {
        this.currency.id(currencyId);
    }

    @Override
    public String getCurrencyCode() {
        return this.currency.code();
    }

    @Override
    public void setCurrencyCode(String code) {
        this.currency.code(code);
    }

    @Override
    public BigDecimal getToVendorRate() {
        return this.currency.toVendorRate();
    }

    @Override
    public void setToVendorRate(BigDecimal rate) {
        this.currency.toVendorRate(rate);
    }

    @Override
    public BigDecimal getFromVendorRate() {
        return this.currency.fromVendorRate();
    }

    @Override
    public void setFromVendorRate(BigDecimal rate) {
        this.currency.fromVendorRate(rate);
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class Currency {
        private Integer id;
        private String code;
        private BigDecimal fromVendorRate;
        private BigDecimal toVendorRate;
    }
}
