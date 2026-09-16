package com.nextgen.gameaggregator.vendor.casinogate.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.casinogate.util.CasinoGateUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .balance(CasinoGateUtils.toMinorUnits(balanceData.getBalance()))
                .buffer(Collections.emptyMap())
                .currency(balanceData.getCurrency())
                .denomination(CasinoGateUtils.getDenomination())
                .build();
    }
}
