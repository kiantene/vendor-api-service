package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

@FunctionalInterface
public interface BetRollbackVendorResponseMapper<R> extends VendorResponseMapper<BetRollbackContext, R> {
    R toVendor(BetRollbackContext context, PlayerBalanceData balanceData);
}
