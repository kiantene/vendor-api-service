package com.nextgen.gameaggregator.core.service.data;

import com.couchbase.client.java.Collection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.AgentPlayer;
import com.nextgen.gameaggregator.repository.ga.writer.AgentPlayerRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
class AgentPlayerCacheService extends CouchbaseCacheService<AgentPlayer> {
    private final AgentPlayerRepository repository;

    public AgentPlayerCacheService(@Qualifier("agentPlayerCollection") Collection agentPlayerCollection,
                                   ObjectMapper objectMapper,
                                   AgentPlayerRepository repository) {

        super(agentPlayerCollection, objectMapper, AgentPlayer.class);
        this.repository = repository;
    }

    public AgentPlayer getById(Long id) {
        String key = buildCacheKey("id", id);
        return retrieve(key,
                () -> repository.findById(id).orElse(null),
                Duration.ofMinutes(120));
    }
}
