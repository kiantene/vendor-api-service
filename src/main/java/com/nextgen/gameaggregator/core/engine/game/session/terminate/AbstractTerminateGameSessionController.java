package com.nextgen.gameaggregator.core.engine.game.session.terminate;

import com.nextgen.gameaggregator.core.common.AbstractFrameworkController;

public abstract class AbstractTerminateGameSessionController<Q, R>
        extends AbstractFrameworkController<Q, R, TerminateGameSessionContext, Void> {

    protected final TerminateGameSessionService gameSessionService;

    protected AbstractTerminateGameSessionController(TerminateGameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @Override
    protected Void executeService(TerminateGameSessionContext context, Q request) {
        gameSessionService.process(context);
        return null;
    }

}
