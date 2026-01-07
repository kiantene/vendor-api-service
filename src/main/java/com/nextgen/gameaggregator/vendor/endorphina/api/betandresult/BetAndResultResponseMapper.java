package com.nextgen.gameaggregator.vendor.endorphina.api.betandresult;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetAndResultResponseMapper implements BetResultVendorResponseMapper<BetAndResultResponse> {
    @Override
    public BetAndResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetAndResultResponse.builder()
                .transactionId(context.getVendorBetId())
                .balance(balanceData.getBalance().setScale(3, RoundingMode.DOWN))
                .build();
    }
}