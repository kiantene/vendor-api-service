package com.nextgen.gameaggregator.vendor.vplus.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.vplus.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.vplus.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.of(balanceData.getBalance());
    }
}
