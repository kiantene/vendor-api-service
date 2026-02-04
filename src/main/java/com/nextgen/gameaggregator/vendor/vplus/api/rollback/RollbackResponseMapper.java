package com.nextgen.gameaggregator.vendor.vplus.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.vplus.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
class RollbackResponseMapper implements BetRollbackVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.of(balanceData.getBalance());
    }
}
