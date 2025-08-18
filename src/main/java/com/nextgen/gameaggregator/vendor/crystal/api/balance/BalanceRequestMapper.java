package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BalanceRequestMapper implements AuthenticateContextMapper<BalanceRequest> {
    @Override
    public AuthenticateContext toInternal(BalanceRequest vendorRequest) {
        return AuthenticateContext.builder()
                .vendorPlayerUsername(vendorRequest.getPlayerId())
                .build();
    }
}