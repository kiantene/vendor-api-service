package com.nextgen.gameaggregator.vendor.vplus.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.vplus.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.of(balanceData.getBalance());
    }
}
