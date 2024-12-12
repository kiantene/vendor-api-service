package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GameServiceImpl implements GameService {

    private final GameSessionService gameSessionService;

    public GameServiceImpl(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @Override
    public GameSession getGameSessionByUsername(String username, String vendorGameCode) throws AuthenticationException {

        GameSession gameSession = gameSessionService.getByVendorPlayerUsername(username, vendorGameCode);

        if (gameSession == null) {
            throw new AuthenticationException();
        }

        return gameSession;
    }
}
