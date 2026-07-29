package com.nextgen.gameaggregator.vendor.mtlive.api.betandresult;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetAndResultResponseMapper implements BetResultVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .timestamp(context.getResultTime())
                .data(SuccessResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
