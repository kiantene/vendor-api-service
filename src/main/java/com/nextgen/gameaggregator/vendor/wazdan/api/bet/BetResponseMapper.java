package com.nextgen.gameaggregator.vendor.wazdan.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.wazdan.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .funds(SuccessResponse.Funds.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}