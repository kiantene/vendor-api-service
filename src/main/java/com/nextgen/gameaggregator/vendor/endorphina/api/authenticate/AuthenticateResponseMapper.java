package com.nextgen.gameaggregator.vendor.endorphina.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import org.springframework.stereotype.Component;


@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<AuthenticateResponse> {
    @Override
    public AuthenticateResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return AuthenticateResponse.builder()
                .player(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrency())
                .game(context.getVendorGameCode())
                .build();
    }
}