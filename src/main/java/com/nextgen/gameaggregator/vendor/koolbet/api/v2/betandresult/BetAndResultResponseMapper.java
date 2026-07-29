package com.nextgen.gameaggregator.vendor.koolbet.api.v2.betandresult;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import org.springframework.stereotype.Component;

@Component
public class BetAndResultResponseMapper implements BetResultVendorResponseMapper<CommonResponse> {

    @Override
    public CommonResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .errorCode(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
