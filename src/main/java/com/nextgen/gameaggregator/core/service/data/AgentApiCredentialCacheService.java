package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.AgentApiCredential;
import com.nextgen.gameaggregator.repository.ga.writer.AgentApiCredentialRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
class AgentApiCredentialCacheService extends CouchbaseCacheService<AgentApiCredential> {

    private final AgentApiCredentialRepository repository;

    AgentApiCredentialCacheService(CouchbaseCacheFactory factory,
                                   AgentApiCredentialRepository repository) {

        super(factory, AgentApiCredential.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("agentId"), Duration.ofMinutes(120)
        );
    }

    public List<AgentApiCredential> getByAgentId(Integer agentId) {
        return repository.findAllByAgentId(agentId);
    }

    public AgentApiCredential getActiveCredential(Integer agentId) {
        String key = buildCacheKey("agentId", agentId);
        return get(key,
                () -> getByAgentId(agentId).stream()
                        .filter(cred -> cred.getStatus() == 1)
                        .findFirst()
                        .orElse(null)
        );
    }
}
