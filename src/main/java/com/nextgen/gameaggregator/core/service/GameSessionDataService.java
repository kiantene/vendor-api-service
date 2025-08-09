package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.repository.ga.writer.RawGameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameSessionDataService {

    private final RawGameSessionRepository repository;

    // TODO: very low probability but token may not be unique, suggest to add vendorId or playerId
    // TODO: move cacheable to GameSessionCacheService
    @Cacheable(value = "GameSessions", key = "#token", cacheManager = "cacheManager")
    public GameSession getByVendorToken(String token) {
        return Optional.ofNullable(repository.findByVendorToken(token))
                .orElseThrow(() -> new GameSessionExpiredException(token + " has expired"));
    }

    // TODO: move cacheable to GameSessionCacheService
    @Cacheable(value = "GameSessions", key = "#token", cacheManager = "cacheManager")
    public GameSession getByToken(String token) {
        return Optional.ofNullable(repository.findByToken(token))
                .orElseThrow(() -> new GameSessionExpiredException(token + " has expired"));
    }

    @Cacheable(value = "GameSessions", key = "#username", cacheManager = "cacheManager")
    public GameSession getByVendorPlayerUsername(String username) {
        validateUsername(username);
        List<GameSession> gameSessionList = repository.findByVendorPlayerUsername(username);

        return findMostRecentGameSession(gameSessionList);
    }

    public void refreshToken(GameSession gameSession) {
        repository.delete(gameSession);
        repository.save(gameSession);
    }

    public boolean shouldRefreshToken(GameSession gameSession) {
        long createTime = gameSession.getCreateTime();
        if (createTime <= 0) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long sessionAgeHours = (currentTime - createTime) / (1000 * 60 * 60); // Convert milliseconds to hours

        return sessionAgeHours < 6;
    }

    // TODO: change to use Couchbase collection mutateIn
    public GameSession updateVendorToken(GameSession gameSession, String newToken) {
        gameSession.setVendorToken(newToken);
        repository.save(gameSession);

        return gameSession;
    }

    private GameSession findMostRecentGameSession(List<GameSession> gameSessionList) {
        if (gameSessionList == null || gameSessionList.isEmpty()) {
            return null;
        }

        return gameSessionList.stream()
                .filter(g -> g != null && g.getCreateTime() != null)
                .max(Comparator.comparingLong(GameSession::getCreateTime))
                .orElse(null);
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
    }
}

