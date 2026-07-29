package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import org.springframework.stereotype.Component;

@Component
public class SessionBetAndResultResponseMapper implements BetVendorResponseMapper<CommonResponse> {

    @Override
    public CommonResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .errorCode(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
