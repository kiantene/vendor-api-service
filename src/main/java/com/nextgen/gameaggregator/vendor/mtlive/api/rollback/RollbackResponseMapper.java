package com.nextgen.gameaggregator.vendor.mtlive.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .timestamp(context.getTimestamp())
                .data(SuccessResponse.Data.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .build();
    }
}
