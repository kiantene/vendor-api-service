package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;

public interface GameService {
    GameSession getGameSessionByUsername(String username, String vendorGameCode) throws AuthenticationException;
}
