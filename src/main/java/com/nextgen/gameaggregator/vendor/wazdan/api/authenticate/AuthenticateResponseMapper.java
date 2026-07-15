package com.nextgen.gameaggregator.vendor.wazdan.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.wazdan.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<AuthenticateResponse> {
    @Override
    public AuthenticateResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return AuthenticateResponse.builder()
                .status(ResponseCode.SUCCESS.code)
                .user(AuthenticateResponse.User.builder()
                        .id(context.getVendorPlayerUsername())
                        .currency(context.getVendorCurrency())
                        .build())
                .funds(AuthenticateResponse.Funds.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
