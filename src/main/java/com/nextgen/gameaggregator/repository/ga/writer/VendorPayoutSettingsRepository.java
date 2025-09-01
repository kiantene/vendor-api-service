package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorPayoutSettings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPayoutSettingsRepository extends JpaRepository<VendorPayoutSettings, Integer> {
    @Cacheable(value = "VendorPayoutSettings", key = "{#masterAgentId, #vendorId, #gameCategoryId, #currencyId}", cacheManager = "cacheManager")
    List<VendorPayoutSettings> findByMasterAgentIdAndVendorIdAndGameCategoryIdAndCurrencyId(Integer masterAgentId, Integer vendorId, Integer gameCategoryId, Integer currencyId);

}
