package com.nextgen.gameaggregator.vendor.groove.api.authenticate;

import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContextMapper;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import org.springframework.stereotype.Component;

@Component
public class AuthenticateRequestMapper implements AuthenticateContextMapper<AuthenticateRequest> {
    @Override
    public AuthenticateContext toInternal(AuthenticateRequest vendorRequest) {
        return AuthenticateContext.builder()
                .vendorPlayerUsername(vendorRequest.getAccountid())
                .vendorSessionToken(vendorRequest.getGamesessionid())
                .token(VendorUtil.extractTokenFromSessionId(vendorRequest.getGamesessionid()))
                .build();
    }
}
