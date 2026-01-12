package com.nextgen.gameaggregator.vendor.spribe.api.v2.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        SuccessResponse.Data data = SuccessResponse.Data.builder()
                .userId(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrency())
                .newBalance(AmountConverter.convertBalanceToUnit(balanceData.getBalance()))
                .build();

        return new SuccessResponse(data);
    }
}
