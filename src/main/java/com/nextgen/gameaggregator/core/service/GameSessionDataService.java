package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSessionDataService {
    private final GameSessionService gameSessionService;

    public GameSession getGameSession(GameSessionData gameSessionData) {
        String token = gameSessionData.getToken();
        if (token != null) {
            return getGameSessionByToken(token);
        }

        String vendorSessionToken = gameSessionData.getVendorSessionToken();
        String vendorPlayerUsername = gameSessionData.getVendorPlayerUsername();
        if (vendorSessionToken != null && vendorPlayerUsername != null) {
            return getGameSessionByUsername(vendorPlayerUsername, vendorSessionToken);
        }

        throw new InvalidRequestException("Session token not present");
    }

    // TODO: very low probability but token may not be unique, suggest to add vendorId or playerId
    // TODO: move caching to GameSessionCacheService
    public GameSession getByVendorToken(String token) {
        try {
            return gameSessionService.verifyVendorToken(token);
        } catch (AuthenticationException ex) {
            throw new GameSessionExpiredException(token + " has expired");
        }
    }

    // TODO: move caching to GameSessionCacheService
    public GameSession getByToken(String token) {
        try {
            return gameSessionService.verifyToken(token);
        } catch (AuthenticationException ex) {
            throw new GameSessionExpiredException(token + " has expired");
        }
    }

    public GameSession getByVendorPlayerUsername(String username) {
        GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(username);

        if (gameSession == null) {
            throw new GameSessionExpiredException("Game session has expired");
        }

        return gameSession;
    }

    private void updateVendorToken(GameSession gameSession, String newToken) {
        // TODO: change to use Couchbase collection mutateIn
        gameSessionService.regenerateVendorToken(gameSession, newToken);
    }

    private GameSession getGameSessionByToken(String token) {
        GameSession gameSession = getByToken(token);
        // TODO: to assess if we need to refresh the token's TTL
//        if (shouldRefreshToken(gameSession)) {
//            refreshToken(gameSession);
//        }

        return gameSession;
    }

    private GameSession getGameSessionByUsername(String vendorPlayerUsername, String vendorSessionToken) {
        GameSession gameSession = getByVendorPlayerUsername(vendorPlayerUsername);

        if (gameSession == null) {
            throw new GameSessionExpiredException("Game session has expired");
        }

        updateVendorToken(gameSession, vendorSessionToken);
        return gameSession;
    }

//    private void refreshToken(GameSession gameSession) {
//        // TODO: use Couchbase collection update ttl
//        repository.delete(gameSession);
//        repository.save(gameSession);
//    }
//
//    private boolean shouldRefreshToken(GameSession gameSession) {
//        long createTime = gameSession.getCreateTime();
//        if (createTime <= 0) {
//            return false;
//        }
//
//        long currentTime = System.currentTimeMillis();
//        long sessionAgeHours = (currentTime - createTime) / (1000 * 60 * 60); // Convert milliseconds to hours
//
//        return sessionAgeHours < 6;
//    }
}

