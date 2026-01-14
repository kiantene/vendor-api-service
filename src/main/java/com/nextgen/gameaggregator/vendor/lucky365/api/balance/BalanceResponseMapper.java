package com.nextgen.gameaggregator.vendor.lucky365.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.List;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<BalanceResponse> {
    @Override
    public BalanceResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return BalanceResponse.builder()
                .code(ResponseCodes.SUCCESS.getCode())
                .data(BalanceResponse.DataInfo.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build()
                )
                .build();
    }
}
