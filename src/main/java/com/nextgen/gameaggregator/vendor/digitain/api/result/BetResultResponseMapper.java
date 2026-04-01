package com.nextgen.gameaggregator.vendor.digitain.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<BetResultResponse> {
    @Override
    public BetResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .txid(context.getVendorBetId())
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .pid(balanceData.getUsername())
                .build();
    }
}