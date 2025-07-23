package com.nextgen.gameaggregator.core.service.data;

import com.nextgen.core.cache.couchbase.CouchbaseCacheFactory;
import com.nextgen.core.cache.couchbase.CouchbaseCacheService;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
class AgentPlayerCacheService extends CouchbaseCacheService<AgentPlayer> {
    private final AgentPlayerRepository repository;

    public AgentPlayerCacheService(CouchbaseCacheFactory factory,
                                   AgentPlayerRepository repository) {

        super(factory, AgentPlayer.class);
        this.repository = repository;
    }

    @Override
    protected Map<String, Duration> getTtlMap() {
        return Map.of(
                ttlKey("id"), Duration.ofMinutes(120)
        );
    }

    public AgentPlayer getById(Long id) {
        String key = buildCacheKey("id", id);
        return get(key, () -> repository.findById(id).orElse(null));
    }
}
