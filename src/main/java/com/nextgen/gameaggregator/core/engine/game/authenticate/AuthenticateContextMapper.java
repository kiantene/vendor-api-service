package com.nextgen.gameaggregator.core.engine.game.authenticate;

@FunctionalInterface
public interface AuthenticateContextMapper<V> {
    AuthenticateContext toAuthenticateContext(V vendorRequest);
}
