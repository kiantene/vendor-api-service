package com.nextgen.gameaggregator.vendor.topbet.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.topbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.topbet.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .merchantTransId(context.getIdempotencyKey())
                .build();
    }
}
