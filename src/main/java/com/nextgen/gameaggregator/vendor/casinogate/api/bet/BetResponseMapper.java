package com.nextgen.gameaggregator.vendor.casinogate.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.casinogate.util.CasinoGateUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .balance(CasinoGateUtils.toMinorUnits(balanceData.getBalance()))
                .buffer(Collections.emptyMap())
                .currency(balanceData.getCurrency())
                .denomination(CasinoGateUtils.getDenomination())
                .build();
    }
}
