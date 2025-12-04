package com.nextgen.gameaggregator.vendor.ezugi.api.v2.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<AuthenticateResponse> {
    @Override
    public AuthenticateResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return AuthenticateResponse.builder()
                .token(context.getVendorSessionToken())
                .uid(balanceData.getUsername())
                .currency(balanceData.getCurrency())
                .balance(balanceData.getBalance())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
