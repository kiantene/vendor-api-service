package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

@FunctionalInterface
public interface BetRollbackContextMapper<V> extends VendorRequestMapper<BetRollbackContext, V> {
    BetRollbackContext toInternal(V vendorRequest);
}
