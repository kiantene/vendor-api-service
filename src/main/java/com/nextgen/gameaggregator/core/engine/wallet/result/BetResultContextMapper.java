package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

public interface BetResultContextMapper<V> extends VendorRequestMapper<BetResultContext, V> {
    @Override
    BetResultContext toInternal(V vendorRequest);
}
