package com.nextgen.gameaggregator.vendor.digitain.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<RollbackResponse> {

    @Override
    public RollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return RollbackResponse.builder()
                .err(ResponseCode.SUCCESS.code)
                .bln(balanceData.getBalance().setScale(4, RoundingMode.DOWN))
                .pid(balanceData.getUsername())
                .rid(context.getRoundId())
                .otxid(context.getTransactionId())
                .build();
    }
}
