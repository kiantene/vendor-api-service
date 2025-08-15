package com.nextgen.gameaggregator.core.engine.wallet.bet;

@FunctionalInterface
public interface BetContextMapper<V> {
    BetContext toBetContext(V vendorRequest);
}
