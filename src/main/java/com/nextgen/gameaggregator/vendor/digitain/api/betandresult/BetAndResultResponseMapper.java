package com.nextgen.gameaggregator.vendor.digitain.api.betandresult;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetAndResultResponseMapper implements BetResultVendorResponseMapper<BetAndResultResponse> {
    @Override
    public BetAndResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetAndResultResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .txid(context.getVendorBetId())
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .pid(balanceData.getUsername())
                .build();
    }
}