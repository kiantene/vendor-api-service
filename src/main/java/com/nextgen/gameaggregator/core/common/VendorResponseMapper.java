package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

@FunctionalInterface
public interface VendorResponseMapper<T, R> {
    R toVendor(T context, PlayerBalanceData balanceData);
}
