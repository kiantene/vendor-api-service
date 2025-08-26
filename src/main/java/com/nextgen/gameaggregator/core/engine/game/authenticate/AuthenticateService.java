package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public interface AuthenticateService {
    PlayerBalanceData process(AuthenticateContext context);
}
