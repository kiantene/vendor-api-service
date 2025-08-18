package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

public interface AuthenticateContextMapper<V> extends VendorRequestMapper<AuthenticateContext, V> {
    @Override
    AuthenticateContext toInternal(V vendorRequest);
}
