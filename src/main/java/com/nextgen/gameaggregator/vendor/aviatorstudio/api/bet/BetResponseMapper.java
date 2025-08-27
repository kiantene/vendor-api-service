package com.nextgen.gameaggregator.vendor.aviatorstudio.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
