package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContextMapper;
import org.springframework.stereotype.Component;

@Component
class AuthenticateRequestMapper implements AuthenticateContextMapper<AuthenticateDto> {
    @Override
    public AuthenticateContext toAuthenticateContext(AuthenticateDto vendorRequest) {
        return AuthenticateContext.builder()
                .vendorSessionToken(vendorRequest.getSessionId())
                .build();
    }
}
