package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.repository.ga.writer.RawGameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSessionDataService {

    private final RawGameSessionRepository repository;

    // TODO: very low probability but token may not be unique, suggest to add vendorId or playerId
    @Cacheable(value = "GameSessions", key = "#token", cacheManager = "cacheManager")
    public GameSession getByVendorToken(String token) {
        return repository.findByVendorToken(token);
    }
}

