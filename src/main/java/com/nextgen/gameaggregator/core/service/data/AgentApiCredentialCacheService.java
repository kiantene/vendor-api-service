package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.repository.ga.writer.AgentApiCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class AgentApiCredentialCacheService {
    private final AgentApiCredentialRepository repository;

    public List<AgentApiCredential> getByAgentId(Integer agentId) {
        return repository.findAllByAgentId(agentId);
    }

    @Cacheable(value = "AgentApiCredentials", key = "#agentId", cacheManager = "cacheManager")
    public AgentApiCredential getActiveCredential(Integer agentId) {
        return getByAgentId(agentId).stream()
                .filter(cred -> cred.getStatus() == 1)
                .findFirst()
                .orElse(null);
    }
}
