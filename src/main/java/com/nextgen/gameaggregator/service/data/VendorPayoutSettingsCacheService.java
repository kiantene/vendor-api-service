package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.ga.VendorPayoutSettings;
import com.nextgen.gameaggregator.repository.ga.writer.VendorPayoutSettingsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorPayoutSettingsCacheService {

    private final VendorPayoutSettingsRepository repository;

    public VendorPayoutSettingsCacheService(VendorPayoutSettingsRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "VendorPayoutSettings", key = "{#masterAgentId, #vendorId, #gameCategoryId, #currencyId}", cacheManager = "cacheManager")
    public List<VendorPayoutSettings> getByMasterAgentId(Integer masterAgentId,
                                                         Integer vendorId,
                                                         Integer gameCategoryId,
                                                         Integer currencyId) {

        return repository.findByMasterAgentIdAndVendorIdAndGameCategoryIdAndCurrencyId(
                masterAgentId, vendorId, gameCategoryId, currencyId
        );
    }
}
