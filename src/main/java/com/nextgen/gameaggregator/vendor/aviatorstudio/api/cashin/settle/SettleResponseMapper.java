package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.settle;

import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInResponse;
import org.springframework.stereotype.Component;

@Component
class SettleResponseMapper implements VendorResponseMapper<BetResultContext, CashInResponse> {
    @Override
    public CashInResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return CashInResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
