package com.nextgen.gameaggregator.vendor.egtdigital.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<BetResultResponse> {
    @Override
    public BetResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        long balance = Amount.vendor(balanceData.getBalance());
        return BetResultResponse.builder()
                .balance(balance)
                .bonusAmount(Amount.vendor(context.getJackpotAmount()))
                .realAmount(Amount.vendor(context.getWinAmount()))
                .casinoTransferId(context.getVendorBetId())
                .statusCode(ResponseCodes.OK.getCode())
                .build();
    }
}