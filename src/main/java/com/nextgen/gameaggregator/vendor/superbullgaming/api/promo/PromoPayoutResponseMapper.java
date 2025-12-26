package com.nextgen.gameaggregator.vendor.superbullgaming.api.promo;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutResponseMapper implements PromoPayoutVendorResponseMapper<PromoPayoutResponse> {
    @Override
    public PromoPayoutResponse toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        return PromoPayoutResponse.builder()
                .balance(balanceData.getBalance())
                .currency(balanceData.getCurrency())
                .username(balanceData.getUsername())
                .build();
    }
}
