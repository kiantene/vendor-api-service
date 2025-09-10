package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.enums.Features;
import org.springframework.stereotype.Service;

@Service
public class VendorFeatureDataService {

    private static final int STATUS_ENABLED = 1;
    private final VendorFeatureCacheService cache;

    public VendorFeatureDataService(VendorFeatureCacheService cache) {
        this.cache = cache;
    }

    public boolean isVendorEnabled(Features feature, Integer vendorId) {
        if (vendorId == null) return false;

        return cache.getByFeatureIdAndVendorIdAndStatus(feature.id, vendorId, STATUS_ENABLED).isPresent();
    }
}
