package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.core.common.VendorResponseMapper;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import org.springframework.stereotype.Component;

@Component
class CashInResponseMapper implements VendorResponseMapper<BetResultContext, CashInResponse> {
    @Override
    public CashInResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return CashInResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
