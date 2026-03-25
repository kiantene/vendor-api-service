package com.nextgen.gameaggregator.vendor.cockfight6.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<CommonSuccessResponse> {
    @Override
    public CommonSuccessResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return CommonSuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .msg(ResponseCode.SUCCESS.message)
                .balance(balanceData.getBalance())
                .build();
    }
}
