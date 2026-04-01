package com.nextgen.gameaggregator.vendor.digitain.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<AuthenticateResponse> {
    @Override
    public AuthenticateResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return AuthenticateResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .tkn(context.getVendorSessionToken())
                .pid(balanceData.getUsername())
                .cid(balanceData.getCurrency())
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .isr(true)
                .build();
    }
}