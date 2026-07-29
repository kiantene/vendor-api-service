package com.nextgen.gameaggregator.vendor.digitain.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return BetResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .txid(context.getVendorBetId())
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .pid(balanceData.getUsername())
                .build();
    }
}
