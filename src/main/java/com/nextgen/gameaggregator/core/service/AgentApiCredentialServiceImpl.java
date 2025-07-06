package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.repository.ga.writer.AgentApiCredentialRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentApiCredentialServiceImpl implements AgentApiCredentialService {

    private final AgentApiCredentialRepository repository;

    public AgentApiCredentialServiceImpl(AgentApiCredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AgentApiCredential> getByAgentId(Integer agentId) {
        return repository.findAllByAgentId(agentId);
    }

    @Cacheable(value = "AgentApiCredentials", key = "#agentId", cacheManager = "cacheManager")
    @Override
    public AgentApiCredential getActiveCredential(Integer agentId) {
        return getByAgentId(agentId).stream()
                .filter(cred -> cred.getStatus() == 1)
                .findFirst()
                .orElse(null);
    }
}
