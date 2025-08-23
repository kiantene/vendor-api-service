package com.nextgen.gameaggregator.vendor.aviatorstudio.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
