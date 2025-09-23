package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorFeatureRepository extends JpaRepository<VendorFeature, Integer> {
    Optional<VendorFeature> findByFeatureIdAndVendorIdAndStatus(Integer featureId, Integer vendorId, Integer status);

}
