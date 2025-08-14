package com.nextgen.gameaggregator.vendor.aviatorstudio.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.result.BetResultResponse;
import org.springframework.stereotype.Component;

@Component
class RollbackResponseMapper implements VendorResponseMapper<BetRollbackContext, BetResultResponse> {
    @Override
    public BetResultResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .id(context.getVendorPlayerUsername())
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
