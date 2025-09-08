package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.enums.FeatureType;
import org.springframework.stereotype.Service;

@Service
public class VendorFeatureDataService {

    private final VendorFeatureCacheService cache;

    public VendorFeatureDataService(VendorFeatureCacheService cache) {
        this.cache = cache;
    }

    public boolean checkIsVendorEnableForMaxPayout(Integer vendorId) {
        return cache.getByVendorIdAndFeatureIdAndStatus(vendorId, FeatureType.MAX_PAYOUT.id, 1).isPresent();
    }
}
