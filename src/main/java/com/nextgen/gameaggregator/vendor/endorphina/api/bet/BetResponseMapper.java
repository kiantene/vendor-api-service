package com.nextgen.gameaggregator.vendor.endorphina.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return BetResponse.builder()
                .transactionId(context.getVendorBetId())
                .balance(balanceData.getBalance().setScale(3, RoundingMode.DOWN))
                .build();
    }
}
