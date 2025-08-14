package com.nextgen.gameaggregator.core.engine.wallet.rollback;

@FunctionalInterface
public interface BetRollbackContextMapper<V> {
    BetRollbackContext toBetRollbackContext(V vendorRequest);
}
