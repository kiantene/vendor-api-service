package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.ga.VendorFeature;
import com.nextgen.gameaggregator.repository.ga.writer.VendorFeatureRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VendorFeatureCacheService {

    private final VendorFeatureRepository repository;

    public VendorFeatureCacheService(VendorFeatureRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "VendorFeatures", key = "{#featureId, #vendorId, #status}", cacheManager = "cacheManager")
    public Optional<VendorFeature> getByFeatureIdAndVendorIdAndStatus(Integer featureId,
                                                                      Integer vendorId,
                                                                      Integer status) {

        return repository.findByFeatureIdAndVendorIdAndStatus(featureId, vendorId, status);
    }
}
