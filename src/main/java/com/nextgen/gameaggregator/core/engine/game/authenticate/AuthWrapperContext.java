package com.nextgen.gameaggregator.core.engine.game.authenticate;

import lombok.Data;

@Data
public class AuthWrapperContext {
    private AuthenticateContext authContext;
    private AuthConfig config;

    public AuthWrapperContext(AuthenticateContext context) {
        this.authContext = context;
        this.config = new AuthConfig();
    }
}
