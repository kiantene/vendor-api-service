package com.nextgen.gameaggregator.vendor.lucky365.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<BetResultResponse> {
    @Override
    public BetResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .code(ResponseCodes.SUCCESS.getCode())
                .data(BetResultResponse.DataInfo.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build()
                )
                .build();
    }
}