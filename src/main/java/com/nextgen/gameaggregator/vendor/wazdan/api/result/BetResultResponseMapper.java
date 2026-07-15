package com.nextgen.gameaggregator.vendor.wazdan.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.wazdan.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .funds(SuccessResponse.Funds.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
