package com.nextgen.gameaggregator.vendor.cockfight6.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.cockfight6.response.CommonSuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class ResultResponseMapper implements BetResultVendorResponseMapper<CommonSuccessResponse> {
    @Override
    public CommonSuccessResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return CommonSuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .msg(ResponseCode.SUCCESS.message)
                .balance(balanceData.getBalance())
                .recordId(Long.valueOf(context.getVendorBetId()))
                .build();
    }
}
