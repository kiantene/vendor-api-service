package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.freeround;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreeRoundPayoutResponseMapper implements PromoPayoutVendorResponseMapper<FreeRoundPayoutResponse> {

    @Override
    public FreeRoundPayoutResponse toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        return FreeRoundPayoutResponse.builder()
                .transactionId(context.getTraceId())
                .currency(context.getVendorCurrency())
                .cash(balanceData.getBalance())
                .bonus(BigDecimal.ZERO)
                .build();
    }
}
