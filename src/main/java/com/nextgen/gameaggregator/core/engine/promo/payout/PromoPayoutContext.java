package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.context.VendorPlayerAware;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.PromoType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Builder
@Data
public class PromoPayoutContext implements VendorPlayerAware {
    // Request
    private String traceId;
    private String idempotencyKey;

    // Vendor Promo values
    private String vendorTransactionId;
    private String vendorPlayerUsername;
    private String vendorCurrency;
    private String vendorCampaignCode;
    private BigDecimal payoutAmount;
    private Long vendorTransactionTime;

    // --- internal values ---
    private PromoType promoType;
    private String transactionId;
    private Integer currencyId;
    private String currencyCode;
    private String campaignUuid;

    // For backward compatibility
    private HttpRequestLog httpRequestLog;

    @Builder.Default
    private Agent agent = new Agent();

    @Builder.Default
    private Vendor vendor = new Vendor();

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

    @Override
    public void setAgentId(Integer agentId) {
        this.agent.id(agentId);
    }

    @Override
    public void setAgentPlayerId(Long agentPlayerId) {
        this.agent.playerId(agentPlayerId);
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
    public void setVendorId(Integer vendorId) {
        this.vendor.id(vendorId);
    }

    @Override
    public void setVendorLineId(Integer vendorLineId) {
        this.vendor.lineId(vendorLineId);
    }
}
