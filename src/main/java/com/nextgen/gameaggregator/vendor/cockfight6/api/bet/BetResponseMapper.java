package com.nextgen.gameaggregator.vendor.cockfight6.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<CommonSuccessResponse> {
    @Override
    public CommonSuccessResponse toVendor(BetContext context, PlayerBalanceData balanceData) {

        return CommonSuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .msg(ResponseCode.SUCCESS.message)
                .balance(balanceData.getBalance())
                .recordId(Long.valueOf(context.getVendorBetId()))
                .build();

    }
}
