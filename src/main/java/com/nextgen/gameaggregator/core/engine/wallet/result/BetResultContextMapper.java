package com.nextgen.gameaggregator.core.engine.wallet.result;

@FunctionalInterface
public interface BetResultContextMapper<V> {
    BetResultContext toBetResultContext(V vendorRequest);
}
