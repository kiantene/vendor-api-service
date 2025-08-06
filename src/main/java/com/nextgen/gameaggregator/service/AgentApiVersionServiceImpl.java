package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentApiVersion;
import com.nextgen.gameaggregator.repository.ga.writer.AgentApiVersionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AgentApiVersionServiceImpl implements AgentApiVersionService {

    private final AgentApiVersionRepository agentApiVersionRepository;

    public AgentApiVersionServiceImpl(AgentApiVersionRepository agentApiVersionRepository) {
        this.agentApiVersionRepository = agentApiVersionRepository;
    }

    @Cacheable(value = "AgentApiVersion", key = "#agentId", cacheManager = "cacheManager")
    @Override
    public Integer getAgentApiVersion(Integer agentId) {
        return agentApiVersionRepository.findById(agentId)
                .map(AgentApiVersion::getApiVersion)
                .orElse(1);
    }

}
