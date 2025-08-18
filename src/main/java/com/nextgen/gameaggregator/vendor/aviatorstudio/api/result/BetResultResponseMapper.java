package com.nextgen.gameaggregator.vendor.aviatorstudio.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import org.springframework.stereotype.Component;

@Component
class BetResultResponseMapper implements BetResultVendorResponseMapper<BetResultResponse> {
    @Override
    public BetResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
