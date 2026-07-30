package com.nextgen.gameaggregator.vendor.koolbet.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import org.springframework.stereotype.Component;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<CommonResponse> {

    @Override
    public CommonResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .errorCode(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .build();
    }
}
