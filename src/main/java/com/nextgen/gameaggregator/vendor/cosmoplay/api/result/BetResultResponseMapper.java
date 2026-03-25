package com.nextgen.gameaggregator.vendor.cosmoplay.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class BetResultResponseMapper implements BetResultVendorResponseMapper<BetResultResponse> {

    @Override
    public BetResultResponse toVendor(BetResultContext context, PlayerBalanceData balanceData) {
        Long balance = Amount.vendor(balanceData.getBalance());

        return BetResultResponse.builder()
                .balance(balance)
                .build();
    }
}
