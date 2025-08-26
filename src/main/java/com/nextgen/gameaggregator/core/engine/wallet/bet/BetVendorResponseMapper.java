package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public interface BetVendorResponseMapper<R> extends VendorResponseMapper<BetContext, R> {
    @Override
    R toVendor(BetContext context, PlayerBalanceData balanceData);
}
