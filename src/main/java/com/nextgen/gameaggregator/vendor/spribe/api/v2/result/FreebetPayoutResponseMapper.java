package com.nextgen.gameaggregator.vendor.spribe.api.v2.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class FreebetPayoutResponseMapper implements PromoPayoutVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        SuccessResponse.Data data = SuccessResponse.Data.builder()
                .userId(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrency())
                .newBalance(AmountConverter.convertBalanceToUnit(balanceData.getBalance()))
                .build();
        return new SuccessResponse(data);
    }
}
