package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

/**
 * Per-vendor mapper: ({@link DebitContext}, {@link PlayerBalanceData}) → vendor wire response {@code R}.
 */
public interface DebitVendorResponseMapper<R> extends VendorResponseMapper<DebitContext, R> {
    @Override
    R toVendor(DebitContext context, PlayerBalanceData balanceData);
}
