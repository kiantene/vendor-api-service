package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public class PromoResponseMapper implements VendorResponseMapper<PromoPayoutContext, CashTransferInOutVo> {
    @Override
    public CashTransferInOutVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        return null;
    }
}
