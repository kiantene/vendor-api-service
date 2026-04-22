package com.nextgen.gameaggregator.vendor.hp100.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.hp100.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<SuccessResponse> {

    @Override
    public SuccessResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .userId(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrency())
                .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN).toString())
                .build();
    }
}
