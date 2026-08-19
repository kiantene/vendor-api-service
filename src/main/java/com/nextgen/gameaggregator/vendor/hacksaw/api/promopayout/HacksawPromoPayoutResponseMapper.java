package com.nextgen.gameaggregator.vendor.hacksaw.api.promopayout;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import org.springframework.stereotype.Component;

@Component
public class HacksawPromoPayoutResponseMapper implements PromoPayoutVendorResponseMapper<ResponseVo> {

    @Override
    public ResponseVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        ResponseVo vo = new ResponseVo();
        vo.setAccountBalance(balanceData.getBalance().longValue());
        vo.setExternalTransactionId(context.getTransactionId());
        return vo;
    }
}
