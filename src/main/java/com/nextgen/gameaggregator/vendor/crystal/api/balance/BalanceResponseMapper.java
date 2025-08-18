package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BalanceResponseMapper implements VendorResponseMapper<AuthenticateContext, BalanceResponse> {
    @Override
    public BalanceResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return BalanceResponse.builder()
                .data(BalanceResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
