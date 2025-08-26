package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.gameaggregator.entity.promo.Campaign;

public interface CampaignService {
    Campaign getCampaign(String vendorCampaignCode, Integer vendorId, String currencyCode);
}
