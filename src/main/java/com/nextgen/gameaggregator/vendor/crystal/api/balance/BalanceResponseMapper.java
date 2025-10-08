package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<BalanceResponse> {
    @Override
    public BalanceResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return BalanceResponse.builder()
                .data(BalanceResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
