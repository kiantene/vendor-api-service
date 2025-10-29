package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.context.VendorCurrencyAware;
import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.PromoType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PromoPayoutContext extends PayoutTransaction implements VendorPlayerAware, VendorCurrencyAware {

    // --- internal values ---
    private PromoType promoType;
    private String campaignUuid;

    private List<PayoutTransaction> payoutTransactions;

    // For backward compatibility
    private HttpRequestLog httpRequestLog;

    @Builder.Default
    private Agent agent = new Agent();

    @Builder.Default
    private Vendor vendor = new Vendor();

    @Builder.Default
    private Currency currency = new Currency();

    @Override
    public Integer getAgentId() {
        return this.agent.id();
    }

    @Override
    public void setAgentId(Integer agentId) {
        this.agent.id(agentId);
    }

    @Override
    public void setAgentPlayerId(Long agentPlayerId) {
        this.agent.playerId(agentPlayerId);
    }

    @Override
    public String getAgentPlayerUsername() {
        return this.agent.playerUsername();
    }

    @Override
    public void setAgentPlayerUsername(String agentPlayerUsername) {
        this.agent.playerUsername(agentPlayerUsername);
    }

    @Override
    public void setVendorPlayerId(Long vendorPlayerId) {
        this.vendor.playerId(vendorPlayerId);
    }

    @Override
    public void setVendorLineId(Integer vendorLineId) {
        this.vendor.lineId(vendorLineId);
    }

    @Override
    public Integer getVendorId() {
        return this.vendor.id();
    }

    @Override
    public void setVendorId(Integer vendorId) {
        this.vendor.id(vendorId);
    }

    @Override
    public String getCurrencyCode() {
        return this.currency.currencyCode();
    }

    @Override
    public void setCurrencyCode(String code) {
        this.currency.currencyCode(code);
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

    @Override
    public Integer getCurrencyId() {
        return this.currency.id();
    }

    @Override
    public void setCurrencyId(Integer currencyId) {
        this.currency.id(currencyId);
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class Agent {
        private Integer id;
        private Integer masterAgentId;
        private Integer houseId;
        private Long playerId;
        private String playerUsername;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class Vendor {
        private Integer id;
        private String code;
        private Integer lineId;
        private Long playerId;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class Currency {
        private Integer id;
        private String currencyCode;
        private BigDecimal fromVendorRate;
        private BigDecimal toVendorRate;
    }
}
