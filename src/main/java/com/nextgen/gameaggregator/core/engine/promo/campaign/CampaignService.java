package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.gameaggregator.entity.promo.Campaign;

import java.util.Map;

public interface CampaignService {
    Campaign getCampaign(String vendorCampaignCode, Integer vendorLineId, Integer promoType);
    Campaign getCampaignByPlayerUuid(String playerUuid);
    Campaign getCampaignByRef(CampaignResolveStrategy strategy, Map<String, String> params);
    Campaign createPayoutCampaign(Integer promoType, Integer vendorId, Integer vendorLineId, String vendorCampaignCode, String vendorCampaignName, String vendorTransactionId, String currencyCode, String vendorCurrencyCode, Long payoutTime);
}
