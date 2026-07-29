package com.nextgen.gameaggregator.vendor.koolbet.api.v2.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .errorCode(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .currency(context.getVendorCurrency())
                .build();
    }
}
