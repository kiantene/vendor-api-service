package com.nextgen.gameaggregator.vendor.hp100.api.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.hp100.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class AuthenticateResponseMapper implements AuthenticateVendorResponseMapper<SuccessResponse> {

    @Override
    public SuccessResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .userId(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrency())
                .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN).toString())
                .userName(context.getVendorPlayerUsername())
                .build();
    }
}
