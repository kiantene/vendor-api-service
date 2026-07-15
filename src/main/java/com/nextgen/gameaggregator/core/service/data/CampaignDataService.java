package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignResolveStrategy;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CampaignDataService {
    private final CampaignCacheService cache;

    public CampaignDataService(CampaignCacheService cache) {
        this.cache = cache;
    }

    public Campaign get(String vendorCampaignCode, Integer vendorLineId, Integer promoType) {
        return Optional.ofNullable(this.cache.getByVendorCampaignCodeAndVendorLineIdAndPromoType(vendorCampaignCode, vendorLineId, promoType))
                .orElseThrow(() -> new EntityNotFoundException(Campaign.class, "vendorCampaignCodeAndVendorLineIdAndPromoType", vendorCampaignCode, vendorLineId, promoType));
    }

    public Campaign getByPlayerUuid(String playerUuid) {
        return Optional.ofNullable(this.cache.getByPlayerUuid(playerUuid))
                .orElseThrow(() -> new EntityNotFoundException(Campaign.class, "playerUuid", playerUuid));
    }

    public Campaign getByRef(CampaignResolveStrategy strategy, Map<String, String> params) {
        return Optional.ofNullable(this.cache.getByRef(strategy, params))
                .orElseThrow(() -> new EntityNotFoundException(Campaign.class, "resolveCampaign", strategy, params));
    }
}
