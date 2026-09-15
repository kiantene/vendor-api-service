package com.nextgen.gameaggregator.vendor.esoterica.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.response.CommonResponse;
import org.springframework.stereotype.Component;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .transactionId(context.getTransactionId())
                .error(ResponseCodes.SUCCESS.getCode())
                .description(ResponseCodes.SUCCESS.getMessage())
                .build();
    }
}
