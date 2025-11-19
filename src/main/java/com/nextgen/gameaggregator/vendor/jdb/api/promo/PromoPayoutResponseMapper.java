package com.nextgen.gameaggregator.vendor.jdb.api.promo;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutResponseMapper implements PromoPayoutVendorResponseMapper<PromoPayoutResponse> {
    @Override
    public PromoPayoutResponse toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        return PromoPayoutResponse.builder()
                .status(ResponseCode.SUCCESS)
                .balance(balanceData.getBalance())
                .errText("")
                .build();
    }
}
