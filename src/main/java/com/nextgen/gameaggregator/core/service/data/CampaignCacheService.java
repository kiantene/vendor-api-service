package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.core.engine.promo.campaign.CampaignService;
import com.nextgen.gameaggregator.entity.promo.Campaign;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        this.enableLocalCache(true);
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("vendorCampaignCodeAndVendorLineIdAndPromoType"), Duration.ofMinutes(120)
        );
    }

    public Campaign getByVendorCampaignCodeAndVendorLineIdAndPromoType(String vendorCampaignCode, Integer vendorLineId, Integer promoType) {
        String key = buildCacheKey("vendorCampaignCodeAndVendorLineIdAndPromoType", vendorCampaignCode, vendorLineId, promoType);
        return get(key, () -> repository.getCampaign(vendorCampaignCode, vendorLineId, promoType));
    }
}
