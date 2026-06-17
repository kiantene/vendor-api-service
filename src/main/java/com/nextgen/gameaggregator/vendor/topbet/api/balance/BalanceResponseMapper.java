package com.nextgen.gameaggregator.vendor.topbet.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.topbet.response.SuccessResponse;
import org.springframework.stereotype.Component;

import static com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil.formatBalance;

@Component
public class BalanceResponseMapper implements BalanceVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BalanceContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .balance(formatBalance(balanceData.getBalance()))
                .build();
    }
}
