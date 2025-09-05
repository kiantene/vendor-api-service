package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.ga.AgentPayoutSetting;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
public class AgentPayoutSettingsDataService {

    private final AgentPayoutSettingsCacheService cache;

    public AgentPayoutSettingsDataService(AgentPayoutSettingsCacheService cache) {
        this.cache = cache;
    }

    @Cacheable(value = "VendorPayoutSettings", key = "{#masterAgentId, #agentId, #vendorId, #gameCategoryId, #currencyId}", cacheManager = "cacheManager")
    public BigDecimal getMaxPayoutAmount(Integer masterAgentId,
                                         Integer agentId,
                                         Integer vendorId,
                                         Integer gameCategoryId,
                                         Integer currencyId) {

        var settingList = cache.getByMasterAgentId(masterAgentId, vendorId, gameCategoryId, currencyId);

        return settingList.stream()
                .filter(v -> v.getStatus() != null && v.getStatus() == 1)
                .filter(v -> v.getAgentId() != null && (v.getAgentId().equals(agentId) || v.getAgentId() == 0))
                .max(Comparator
                        .comparingInt((AgentPayoutSetting v) -> v.getAgentId().equals(agentId) ? 1 : 0) // agent priority
                        .thenComparingInt(AgentPayoutSetting::getVersion) // highest version next
                )
                .map(AgentPayoutSetting::getMaxPayout)
                .orElse(null);
    }
}
