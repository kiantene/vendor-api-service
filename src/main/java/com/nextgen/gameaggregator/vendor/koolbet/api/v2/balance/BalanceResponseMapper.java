package com.nextgen.gameaggregator.vendor.koolbet.api.v2.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import org.springframework.stereotype.Component;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<CommonResponse> {
    @Override
    public CommonResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return CommonResponse.builder()
                .errorCode(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .username(context.getVendorPlayerUsername())
                .balance(balanceData.getBalance())
                .currency(context.getVendorCurrency())
                .build();
    }
}
