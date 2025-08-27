package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;

public interface PromoPayoutContextMapper<V> extends VendorRequestMapper<PromoPayoutContext, V> {
    @Override
    PromoPayoutContext toInternal(V vendorRequest);
}
