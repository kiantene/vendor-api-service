package com.nextgen.gameaggregator.vendor.casinogate.api.refund;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.casinogate.response.CommonResponse;
import com.nextgen.gameaggregator.vendor.casinogate.util.CasinoGateUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class RefundResponseMapper implements BetRollbackVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .balance(CasinoGateUtils.toMinorUnits(balanceData.getBalance()))
                .buffer(Collections.emptyMap())
                .currency(balanceData.getCurrency())
                .denomination(CasinoGateUtils.getDenomination())
                .build();
    }
}
