package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

@FunctionalInterface
public interface BalanceContextMapper<V> extends VendorRequestMapper<BalanceContext, V> {
    BalanceContext toInternal(V vendorRequest);
}
