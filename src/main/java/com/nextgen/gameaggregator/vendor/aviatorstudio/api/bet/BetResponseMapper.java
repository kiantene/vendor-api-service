package com.nextgen.gameaggregator.vendor.aviatorstudio.api.bet;

import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import org.springframework.stereotype.Component;

@Component
class BetResponseMapper implements VendorResponseMapper<BetContext, BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return BetResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
