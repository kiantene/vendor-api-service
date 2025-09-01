package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.VendorPayoutSettings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class VendorPayoutSettingsDataService {

    private final VendorPayoutSettingsCacheService vendorPayoutSettingsCacheService;

    public VendorPayoutSettingsDataService(VendorPayoutSettingsCacheService vendorPayoutSettingsCacheService) {
        this.vendorPayoutSettingsCacheService = vendorPayoutSettingsCacheService;
    }

    @Cacheable(value = "VendorPayoutSettings", key = "{#masterAgentId, #agentId, #vendorId, #gameCategoryId, #currencyId}", cacheManager = "cacheManager")
    public BigDecimal getMaxPayoutAmount(Integer masterAgentId,
                                         Integer agentId,
                                         Integer vendorId,
                                         Integer gameCategoryId,
                                         Integer currencyId) {

        List<VendorPayoutSettings> vendorPayoutSettingsList =
                vendorPayoutSettingsCacheService.getByMasterAgentId(masterAgentId, vendorId, gameCategoryId, currencyId);

        return vendorPayoutSettingsList.stream()
                .filter(v -> v.getStatus() != null && v.getStatus() == 1)
                .filter(v -> v.getAgentId() != null && (v.getAgentId().equals(agentId) || v.getAgentId() == 0))
                .max(Comparator
                        .comparingInt((VendorPayoutSettings v) -> v.getAgentId().equals(agentId) ? 1 : 0) // agent priority
                        .thenComparingInt(VendorPayoutSettings::getVersion) // highest version next
                )
                .map(VendorPayoutSettings::getMaxPayout)
                .orElse(null);

    }
}
