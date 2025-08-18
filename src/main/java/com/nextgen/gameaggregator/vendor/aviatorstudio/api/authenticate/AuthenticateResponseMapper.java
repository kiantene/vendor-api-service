package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import org.springframework.stereotype.Component;

@Component
class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<AuthenticateResponse> {
    @Override
    public AuthenticateResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return AuthenticateResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
