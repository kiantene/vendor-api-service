package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

/**
 * Per-vendor mapper: ({@link CreditContext}, {@link PlayerBalanceData}) → vendor wire response {@code R}.
 */
public interface CreditVendorResponseMapper<R> extends VendorResponseMapper<CreditContext, R> {
    @Override
    R toVendor(CreditContext context, PlayerBalanceData balanceData);
}
