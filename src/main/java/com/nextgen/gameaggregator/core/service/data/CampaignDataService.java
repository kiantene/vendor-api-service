package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CampaignDataService {
    private final CampaignCacheService cache;

    public CampaignDataService(CampaignCacheService cache) {
        this.cache = cache;
    }

    public Campaign get(String vendorCampaignCode, Integer vendorId, String currencyCode) {
        return Optional.ofNullable(this.cache.getByVendorCampaignCodeAndVendorIdAndCurrencyCode(vendorCampaignCode, vendorId, currencyCode))
                .orElseThrow(() -> new EntityNotFoundException(Campaign.class, "vendorCampaignCodeAndVendorIdAndCurrencyCode", vendorCampaignCode, vendorId, currencyCode));
    }
}
