package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.gameaggregator.entity.promo.Campaign;

import java.util.Map;

public interface CampaignService {
    Campaign getCampaign(String vendorCampaignCode, Integer vendorLineId, Integer promoType);
    Campaign getCampaignByPlayerUuid(String playerUuid);
    Campaign getCampaignByRef(CampaignResolveStrategy strategy, Map<String, String> params);
}
