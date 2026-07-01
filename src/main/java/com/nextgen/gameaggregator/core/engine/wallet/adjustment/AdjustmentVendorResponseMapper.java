package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public interface AdjustmentVendorResponseMapper<R> extends VendorResponseMapper<AdjustmentContext, R> {
    R toVendor(AdjustmentContext context, PlayerBalanceData balanceData);
}
