package com.nextgen.gameaggregator.vendor.cosmoplay.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<RollbackResponse> {
    @Override
    public RollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        Long balance = Amount.vendor(balanceData.getBalance());

        return RollbackResponse.builder()
                .balance(balance)
                .build();
    }
}
