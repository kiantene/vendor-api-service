package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignResolveStrategy;
import lombok.Data;

@Data
public class PromoPayoutConfig {
    private boolean batch;
    private boolean playerUuidCampaignLookup;
    /** Non-null opts this vendor into the generic /resolveCampaign endpoint with this strategy. */
    private CampaignResolveStrategy campaignResolveStrategy;

    public PromoPayoutConfig() {
        this.batch = false;
        this.playerUuidCampaignLookup = false;
        this.campaignResolveStrategy = null;
    }

    public PromoPayoutConfig batch(boolean flag) {
        this.batch = flag;
        return this;
    }

    public PromoPayoutConfig playerUuidCampaignLookup(boolean flag) {
        this.playerUuidCampaignLookup = flag;
        return this;
    }

    public PromoPayoutConfig campaignResolveStrategy(CampaignResolveStrategy strategy) {
        this.campaignResolveStrategy = strategy;
        return this;
    }
}
