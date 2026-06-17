package com.nextgen.gameaggregator.vendor.topbet.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.topbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.topbet.response.SuccessResponse;
import org.springframework.stereotype.Component;

import static com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil.formatBalance;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<SuccessResponse> {
    @Override
    public SuccessResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return SuccessResponse.builder()
                .code(ResponseCode.SUCCESS.code)
                .message(ResponseCode.SUCCESS.message)
                .merchantTransId(context.getIdempotencyKey())
                .balance(formatBalance(balanceData.getBalance()))
                .build();
    }
}