package com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<BetResponse> {
    @Override
    public BetResponse toVendor(BetContext context, PlayerBalanceData balanceData) {
        return BetResponse.builder()
                .token(context.getVendorSessionToken())
                .transactionId(context.getIdempotencyKey())
                .uid(balanceData.getUsername())
                .currency(balanceData.getCurrency())
                .balance(balanceData.getBalance())
                .timestamp(balanceData.getTimestamp())
                .build();
    }
}
