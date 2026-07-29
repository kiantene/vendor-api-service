package com.nextgen.gameaggregator.core.engine.game.session;

import com.nextgen.gameaggregator.core.common.AbstractFrameworkController;
import com.nextgen.gameaggregator.entity.ga.GameSession;

/**
 * A specialized controller for Game Session Refresh flows.
 * * It fixes the internal Context to {@link GameSessionRefreshContext} and the 
 * internal Result to {@link GameSession}, but allows specific vendor implementations 
 * to define their own Request (Q) and Response (R) types.
 *
 * @param <Q> The Vendor-specific Refresh Request payload.
 * @param <R> The Vendor-specific Refresh Response payload.
 */
public abstract class AbstractGameSessionRefreshController<Q, R> 
    extends AbstractFrameworkController<Q, R, GameSessionRefreshContext, GameSession> {
    
    protected final GameSessionRefreshService refreshService;

    protected AbstractGameSessionRefreshController(GameSessionRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Override
    protected GameSession executeService(GameSessionRefreshContext context, Q request) {
        return refreshService.execute(context);
    }
}
