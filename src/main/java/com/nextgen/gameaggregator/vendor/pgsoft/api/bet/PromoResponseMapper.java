package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public class PromoResponseMapper implements VendorResponseMapper<PromoPayoutContext, CashTransferInOutVo> {
    @Override
    public CashTransferInOutVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        CashTransferInOutVo vo = new CashTransferInOutVo();
        vo.setCurrencyCode(context.getVendorCurrency());
        vo.setBalanceAmount(balanceData.getBalance());
        vo.setUpdatedTime(balanceData.getTimestamp());
        return vo;
    }
}
