package com.nextgen.gameaggregator.core.service;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.repository.ga.writer.RawGameSessionRepository;
import com.nextgen.gameaggregator.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameSessionDataService {
    private final GameSessionService gameSessionService;
    private final RawGameSessionRepository repository;

    /**
     * Retrieves game session using available session identifiers in priority order:
     * 1. GA session token
     * 2. Vendor session token
     * 3. Vendor player username, if both tokens are not present in the request
     */
    public GameSession getGameSession(VendorRequestContext context) {
        String token = context.getToken();
        if (token != null) {
            GameSession session = getByToken(token, context);
            if (session != null) return session;
        }

        String vendorSessionToken = context.getVendorSessionToken();
        if (vendorSessionToken != null) {
            GameSession session = getByVendorToken(vendorSessionToken, context);
            if (session != null) return session;
        }

        String vendorPlayerUsername = context.getVendorPlayerUsername();
        if (vendorPlayerUsername != null) {
            GameSession session = getByVendorPlayerUsername(vendorPlayerUsername, context);
            if (session != null) return session;
        }

        throw new GameSessionExpiredException();
    }

    public GameSession getOrCreate(VendorRequestContext context) {
        try {
            return getGameSession(context);
        } catch (GameSessionExpiredException ex) {
            // regenerate only when username is available
            // some vendors don't provide username on api request, they only provide token
            // TODO: what if vendor sends a result, but token is not available due to TTL and no username present in request to regenerate token?
            if (context.getVendorPlayerUsername() != null) {
                return regenerateGameSession(context);
            } else {
                throw ex;
            }
        }
    }

    private GameSession regenerateGameSession(VendorRequestContext context) {
        String vendorPlayerUsername = context.getVendorPlayerUsername();
        try {
            GameSession gameSession = gameSessionService.generateNewSessionToken(vendorPlayerUsername);
            String vendorGameCode = context.getVendorGameCode();
            if (vendorGameCode != null && !vendorGameCode.isBlank()) {
                gameSessionService.updateByVendorGameCode(gameSession, vendorGameCode);
            }
            gameSessionService.updateByVendorCurrencyId(gameSession);
            gameSession.setToken(context.getToken());
            gameSession.setVendorToken(context.getVendorSessionToken());

            return gameSession;
        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage(), ex);
        }
    }

    // TODO: move caching to GameSessionCacheService
    public GameSession getByToken(String token, VendorRequestContext context) {
        try {
            return gameSessionService.verifyToken(token);
        } catch (AuthenticationException ex) {
            throw new GameSessionExpiredException(context, token + " has expired");
        }
    }

    // TODO: very low probability but token may not be unique, suggest to add vendorId or playerId
    // TODO: move caching to GameSessionCacheService
    public GameSession getByVendorToken(String token, VendorRequestContext context) {
        try {
            return gameSessionService.verifyVendorToken(token);
        } catch (AuthenticationException ex) {
            throw new GameSessionExpiredException(context, token + " has expired");
        }
    }

    @Cacheable(value = "GameSessions", key = "#username", cacheManager = "cacheManager")
    public GameSession getByVendorPlayerUsername(String username, VendorRequestContext context) {
        List<GameSession> gameSessionList = repository.findByVendorPlayerUsername(username);

        if (gameSessionList.isEmpty()) {
            throw new GameSessionExpiredException(context, "Game session has expired");
        }

        // Return the player's latest launched session regardless of the request game code.
        // A Vendor Lobby ("More Games") switch sends a game code different from the launched
        // game; filtering by it here would drop the real session and force a lossy rebuild
        // (missing platformId/languageId). Game-code reconciliation is done at validation
        // time on a request-local copy (see BetValidator), so the real session is preserved.
        return gameSessionList.stream()
                .max(Comparator.comparingLong(GameSession::getCreateTime))
                .orElseThrow(() -> new GameSessionExpiredException(context, "Game session has expired"));
    }

    public void updateVendorToken(GameSession gameSession, String newToken) {
        // TODO: change to use Couchbase collection mutateIn
        gameSessionService.regenerateVendorToken(gameSession, newToken);
    }

    public GameSession createNewGameSession(GameSession gameSession) {
        try {
            String newToken = UUID.randomUUID().toString();
            gameSession.setId(newToken);
            gameSession.setToken(newToken);
            gameSession.setVendorToken(newToken);

            return gameSessionService.updateSession(gameSession);

        } catch (Exception ex) {
            throw new InternalServerException(
                    "Failed to create new game session for user: " + gameSession.getVendorPlayerUsername(), ex
            );
        }
    }

    public void terminateGameSession(GameSession session) {
        //session already terminated if status = 0
        if (session.getStatus() == 0) {
            return;
        }
        gameSessionService.clearGameSession(
                session,
                session.getAgentPlayerUsername(),
                session.getVendorGameCode()
        );
    }
}
