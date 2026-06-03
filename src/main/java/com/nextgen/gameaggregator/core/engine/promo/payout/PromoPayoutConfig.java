package com.nextgen.gameaggregator.core.engine.promo.payout;

import lombok.Data;

@Data
public class PromoPayoutConfig {
    private boolean batch;
    private boolean playerUuidCampaignLookup;
    /**
     * When true, zero payout still invokes operator promo payout (e.g. free round loss).
     */
    private boolean callOperatorOnZeroPayout;

    public PromoPayoutConfig() {
        this.batch = false;
        this.playerUuidCampaignLookup = false;
        this.callOperatorOnZeroPayout = false;
    }

    public PromoPayoutConfig batch(boolean flag) {
        this.batch = flag;
        return this;
    }

    public PromoPayoutConfig playerUuidCampaignLookup(boolean flag) {
        this.playerUuidCampaignLookup = flag;
        return this;
    }
    
    public PromoPayoutConfig callOperatorOnZeroPayout(boolean flag) {
        this.callOperatorOnZeroPayout = flag;
        return this;
    }
}
