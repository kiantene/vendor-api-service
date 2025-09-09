package com.nextgen.gameaggregator.service.data;

import org.springframework.stereotype.Service;

@Service
public class VendorFeatureDataService {

    private static final int STATUS_ENABLED = 1;
    private final VendorFeatureCacheService cache;

    public VendorFeatureDataService(VendorFeatureCacheService cache) {
        this.cache = cache;
    }

    public boolean isVendorEnabled(Integer vendorId, Integer featureId) {
        return cache.getByVendorIdAndFeatureIdAndStatus(vendorId, featureId, STATUS_ENABLED).isPresent();

    }
}
