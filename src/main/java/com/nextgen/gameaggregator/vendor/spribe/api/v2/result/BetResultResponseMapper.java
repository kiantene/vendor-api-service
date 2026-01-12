package com.nextgen.gameaggregator.vendor.spribe.api.v2.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        SuccessResponse.Data data = SuccessResponse.Data.builder()
                .userId(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrency())
                .newBalance(AmountConverter.convertBalanceToUnit(balanceData.getBalance()))
                .build();

        return new SuccessResponse(data);
    }
}
