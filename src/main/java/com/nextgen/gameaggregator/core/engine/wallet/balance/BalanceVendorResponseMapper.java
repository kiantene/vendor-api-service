package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

@FunctionalInterface
public interface BalanceVendorResponseMapper<R> extends VendorResponseMapper<BalanceContext, R> {
    R toVendor(BalanceContext context, PlayerBalanceData balanceData);
}
