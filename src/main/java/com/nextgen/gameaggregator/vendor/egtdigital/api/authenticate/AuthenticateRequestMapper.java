package com.nextgen.gameaggregator.vendor.egtdigital.api.authenticate;

import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContextMapper;
import org.springframework.stereotype.Component;

@Component
public class AuthenticateRequestMapper implements AuthenticateContextMapper<AuthenticateRequest> {
    @Override
    public AuthenticateContext toInternal(AuthenticateRequest vendorRequest) {
        return AuthenticateContext.builder()
                .vendorSessionToken(vendorRequest.getDefenceCode())
                .build();
    }
}
