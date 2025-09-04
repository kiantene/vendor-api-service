package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class CampaignCacheService extends CouchbaseCacheService<Campaign> {
    private final CampaignService repository;

    public CampaignCacheService(CouchbaseCacheFactory factory, CampaignService repository) {
        super(factory, Campaign.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("vendorCampaignCodeAndVendorIdAndCurrencyCode"), Duration.ofMinutes(120)
        );
    }

    public Campaign getByVendorCampaignCodeAndVendorIdAndCurrencyCode(String vendorCampaignCode, Integer vendorId, String currencyCode) {
        String key = buildCacheKey("vendorCampaignCodeAndVendorIdAndCurrencyCode", vendorCampaignCode, vendorId, currencyCode);
        return get(key, () -> repository.getCampaign(vendorCampaignCode, vendorId, currencyCode));
    }
}
