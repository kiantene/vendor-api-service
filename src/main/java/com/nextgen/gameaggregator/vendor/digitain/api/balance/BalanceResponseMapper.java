package com.nextgen.gameaggregator.vendor.digitain.api.balance;


import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<BalanceResponse> {
    @Override
    public BalanceResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return BalanceResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .build();
    }
}
