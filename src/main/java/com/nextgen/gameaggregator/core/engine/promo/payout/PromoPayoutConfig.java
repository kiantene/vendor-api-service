package com.nextgen.gameaggregator.core.engine.promo.payout;

import lombok.Data;

@Data
public class PromoPayoutConfig {
    private boolean batch;
    private boolean playerUuidCampaignLookup;

    public PromoPayoutConfig() {
        this.batch = false;
        this.playerUuidCampaignLookup = false;
    }

    public PromoPayoutConfig batch(boolean flag) {
        this.batch = flag;
        return this;
    }

    public PromoPayoutConfig playerUuidCampaignLookup(boolean flag) {
        this.playerUuidCampaignLookup = flag;
        return this;
    }
}
