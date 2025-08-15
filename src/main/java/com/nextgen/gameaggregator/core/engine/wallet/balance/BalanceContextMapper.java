package com.nextgen.gameaggregator.core.engine.wallet.balance;

@FunctionalInterface
public interface BalanceContextMapper<V> {
    BalanceContext toBalanceContext(V vendorRequest);
}
