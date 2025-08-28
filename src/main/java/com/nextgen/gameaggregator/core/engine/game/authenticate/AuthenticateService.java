package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.Consumer;

public interface AuthenticateService {
    AuthenticateService initialise(AuthenticateContext context);
    AuthenticateService configure(Consumer<AuthConfig> config);
    PlayerBalanceData process();
}
