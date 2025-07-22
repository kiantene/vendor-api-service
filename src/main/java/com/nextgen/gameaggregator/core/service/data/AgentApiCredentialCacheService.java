package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.repository.ga.writer.AgentApiCredentialRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
class AgentApiCredentialCacheService extends CouchbaseCacheService<AgentApiCredential> {
    private final Map<String, Duration> ttl = Map.of(
            "agentId", Duration.ofMinutes(120)
    );

    private final AgentApiCredentialRepository repository;

    AgentApiCredentialCacheService(CacheCollectionFactory factory,
                                   AgentApiCredentialRepository repository) {

        super(factory.get(AgentApiCredential.class), AgentApiCredential.class);
        this.repository = repository;
    }

    public List<AgentApiCredential> getByAgentId(Integer agentId) {
        return repository.findAllByAgentId(agentId);
    }

    public AgentApiCredential getActiveCredential(Integer agentId) {
        String key = buildCacheKey("agentId", agentId);
        return retrieve(key,
                () -> getByAgentId(agentId).stream()
                        .filter(cred -> cred.getStatus() == 1)
                        .findFirst()
                        .orElse(null),
                ttl.get("agentId"));
    }
}
