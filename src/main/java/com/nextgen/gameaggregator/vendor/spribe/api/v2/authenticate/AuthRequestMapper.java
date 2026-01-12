package com.nextgen.gameaggregator.vendor.spribe.api.v2.authenticate;

import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContextMapper;
import org.springframework.stereotype.Component;

@Component
public class AuthRequestMapper implements AuthenticateContextMapper<AuthRequest> {
    @Override
    public AuthenticateContext toInternal(AuthRequest request) {
        return AuthenticateContext.builder()
                .vendorCurrency(request.getCurrency())
                .vendorSessionToken(request.getSessionToken())
                .token(request.getUserToken())
                .build();
    }
}
