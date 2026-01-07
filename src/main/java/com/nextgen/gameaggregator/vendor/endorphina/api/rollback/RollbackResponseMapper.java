package com.nextgen.gameaggregator.vendor.endorphina.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<RollbackResponse> {

    @Override
    public RollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {

        return RollbackResponse.builder()
                .transactionId(context.getVendorBetId())
                .balance(balanceData.getBalance().setScale(3, RoundingMode.DOWN))
                .build();
    }
}
