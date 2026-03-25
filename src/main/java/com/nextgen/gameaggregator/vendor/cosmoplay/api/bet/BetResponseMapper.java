package com.nextgen.gameaggregator.vendor.cosmoplay.api.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        Long balance = Amount.vendor(balanceData.getBalance());

        return BetResponse.builder()
                .balance(balance)
                .build();
    }
}
