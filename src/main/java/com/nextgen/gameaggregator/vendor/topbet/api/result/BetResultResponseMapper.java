package com.nextgen.gameaggregator.vendor.topbet.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.topbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.topbet.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .merchantTransId(context.getIdempotencyKey())
                .build();
    }
}
