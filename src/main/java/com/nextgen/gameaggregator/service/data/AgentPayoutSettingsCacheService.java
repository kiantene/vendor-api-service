package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.ga.AgentPayoutSetting;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPayoutSettingRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentPayoutSettingsCacheService {

    private final AgentPayoutSettingRepository repository;

    public AgentPayoutSettingsCacheService(AgentPayoutSettingRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "VendorPayoutSettings", key = "{#masterAgentId, #vendorId, #gameCategoryId, #currencyId}", cacheManager = "cacheManager")
    public List<AgentPayoutSetting> getByMasterAgentId(Integer masterAgentId,
                                                       Integer vendorId,
                                                       Integer gameCategoryId,
                                                       Integer currencyId) {

        return repository.findByMasterAgentIdAndVendorIdAndGameCategoryIdAndCurrencyId(
                masterAgentId, vendorId, gameCategoryId, currencyId
        );
    }
}
