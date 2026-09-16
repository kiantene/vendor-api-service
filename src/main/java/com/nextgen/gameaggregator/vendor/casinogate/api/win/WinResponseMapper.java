package com.nextgen.gameaggregator.vendor.casinogate.api.win;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.casinogate.util.CasinoGateUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class WinResponseMapper implements BetResultVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .balance(CasinoGateUtils.toMinorUnits(balanceData.getBalance()))
                .buffer(Collections.emptyMap())
                .currency(balanceData.getCurrency())
                .denomination(CasinoGateUtils.getDenomination())
                .build();
    }
}
