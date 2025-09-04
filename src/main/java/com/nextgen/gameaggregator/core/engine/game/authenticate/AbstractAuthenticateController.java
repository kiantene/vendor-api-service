package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public abstract class AbstractAuthenticateController<Q, R> extends AbstractProcessorController<Q, R, AuthenticateContext> {
    protected final AuthenticateService authenticateService;

    protected AbstractAuthenticateController(AuthenticateContextMapper<Q> requestMapper,
                                             AuthenticateVendorResponseMapper<R> responseMapper,
                                             AuthenticateService authenticateService) {
        super(requestMapper, responseMapper);
        this.authenticateService = authenticateService;
    }

    @Override
    protected PlayerBalanceData executeService(AuthenticateContext context) {
        return authenticateService.process(context);
    }
}
