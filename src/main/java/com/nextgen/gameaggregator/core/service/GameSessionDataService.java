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

    /**
     * Retrieves game session using available session identifiers in priority order:
     * 1. GA session token
     * 2. Vendor session token
     * 3. Vendor player username, if both tokens are not present in the request
     */
    public GameSession getGameSession(GameSessionData gameSessionData) {
        String token = gameSessionData.getToken();
        if (token != null) {
            GameSession session = getByToken(token);
            if (session != null) return session;
        }

        String vendorSessionToken = gameSessionData.getVendorSessionToken();
        if (vendorSessionToken != null) {
            GameSession session = getByVendorToken(vendorSessionToken);
            if (session != null) return session;
        }

        String vendorPlayerUsername = gameSessionData.getVendorPlayerUsername();
        if (vendorPlayerUsername != null) {
            GameSession session = getByVendorPlayerUsername(vendorPlayerUsername);
            if (session != null) return session;
        }

        throw new GameSessionExpiredException();
    }

    public GameSession getOrCreate(GameSessionData gameSessionData) {
        try {
            return getGameSession(gameSessionData);
        } catch (GameSessionExpiredException ex) {
            // TODO: create new with vendorUsername
            return new GameSession();
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

    // TODO: very low probability but token may not be unique, suggest to add vendorId or playerId
    // TODO: move caching to GameSessionCacheService
    public GameSession getByVendorToken(String token) {
        try {
            return gameSessionService.verifyVendorToken(token);
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

    public void updateVendorToken(GameSession gameSession, String newToken) {
        // TODO: change to use Couchbase collection mutateIn
        gameSessionService.regenerateVendorToken(gameSession, newToken);
    }
}

