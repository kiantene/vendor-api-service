package com.nextgen.gameaggregator.vendor.cockfight6.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<CommonSuccessResponse> {
    @Override
    public CommonSuccessResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return CommonSuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .msg(ResponseCode.SUCCESS.message)
                .balance(balanceData.getBalance())
                .recordId(Long.valueOf(context.getVendorBetId()))
                .build();
    }
}
