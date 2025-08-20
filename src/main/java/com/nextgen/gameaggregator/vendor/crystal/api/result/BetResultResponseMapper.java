package com.nextgen.gameaggregator.vendor.crystal.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<BetResultResponse> {
    @Override
    public BetResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .data(BetResultResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .actionId(context.getVendorBetId())
                        .build())
                .build();
    }
}