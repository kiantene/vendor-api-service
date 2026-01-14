package com.nextgen.gameaggregator.vendor.lucky365.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.lucky365.api.result.BetResultResponse;
import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<RollbackResponse> {

    @Override
    public RollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {

        return RollbackResponse.builder()
                .code(ResponseCodes.SUCCESS.getCode())
                .data(RollbackResponse.DataInfo.builder()
                        .balance(balanceData.getBalance().setScale(2, RoundingMode.DOWN))
                        .build()
                )
                .build();
    }
}
