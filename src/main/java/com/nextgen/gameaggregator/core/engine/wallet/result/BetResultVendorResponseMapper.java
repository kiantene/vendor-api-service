package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public interface BetResultVendorResponseMapper<R> extends VendorResponseMapper<BetResultContext, R> {
    @Override
    R toVendor(BetResultContext context, PlayerBalanceData balanceData);
}
