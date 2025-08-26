package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public interface PromoPayoutVendorResponseMapper<R> extends VendorResponseMapper<PromoPayoutContext, R> {
    @Override
    R toVendor(PromoPayoutContext context, PlayerBalanceData balanceData);
}
