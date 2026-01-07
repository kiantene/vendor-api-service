package com.nextgen.gameaggregator.vendor.endorphina.api.result;

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
                .transactionId(context.getVendorBetId())
                .balance(balanceData.getBalance().setScale(3, RoundingMode.DOWN))
                .build();
    }
}