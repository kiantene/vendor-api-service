package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashout;

import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import org.springframework.stereotype.Component;

@Component
class CashOutResponseMapper implements VendorResponseMapper<BetContext, CashOutResponse> {
    @Override
    public CashOutResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return CashOutResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
