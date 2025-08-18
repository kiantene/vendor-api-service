package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

public interface BetContextMapper<V> extends VendorRequestMapper<BetContext, V> {
    @Override
    BetContext toInternal(V vendorRequest);
}
