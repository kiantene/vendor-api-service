package com.nextgen.gameaggregator.vendor.lucky365.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return BetResponse.builder()
                .code(ResponseCodes.SUCCESS.getCode())
                .data(BetResponse.DataInfo.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build()
                )
                .build();
    }
}
