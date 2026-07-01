package com.nextgen.gameaggregator.vendor.mtlive.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.Instant;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .timestamp(Instant.now().getEpochSecond())
                .data(SuccessResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
