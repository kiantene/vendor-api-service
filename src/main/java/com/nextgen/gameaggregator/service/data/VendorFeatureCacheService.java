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

    @Cacheable(value = "VendorFeatures", key = "{#vendorId, #featureId, #status}", cacheManager = "cacheManager")
    public Optional<VendorFeature> getByVendorIdAndFeatureIdAndStatus(Integer vendorId,
                                                                      Integer featureId,
                                                                      Integer status) {

        return repository.findByVendorIdAndFeatureIdAndStatus(vendorId, featureId, status);
    }
}
