package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInResponse;
import org.springframework.stereotype.Component;

@Component
class RollbackResponseMapper implements VendorResponseMapper<BetRollbackContext, CashInResponse> {
    @Override
    public CashInResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return CashInResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
