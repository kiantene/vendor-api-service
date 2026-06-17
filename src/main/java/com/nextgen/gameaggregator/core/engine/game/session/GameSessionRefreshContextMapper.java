package com.nextgen.gameaggregator.core.engine.game.session;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

public interface GameSessionRefreshContextMapper<V> extends VendorRequestMapper<GameSessionRefreshContext, V> {
    @Override
    GameSessionRefreshContext toInternal(V request);
}
