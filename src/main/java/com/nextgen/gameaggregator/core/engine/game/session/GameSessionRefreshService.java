package com.nextgen.gameaggregator.core.engine.game.session;

import com.nextgen.gameaggregator.entity.ga.GameSession;

public interface GameSessionRefreshService {
    GameSession execute(GameSessionRefreshContext context);
}
