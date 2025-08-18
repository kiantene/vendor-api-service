package com.nextgen.gameaggregator.vendor.crystal.api.settle;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class SettleRequestMapper implements BetResultContextMapper<SettleRequest> {
    @Override
    public BetResultContext toBetResultContext(SettleRequest vendorRequest) {
        return BetResultContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .gameCode(vendorRequest.getGameCode())
                .roundId(vendorRequest.getRoundId())
                .vendorBetId(vendorRequest.getTransactionId())
                .vendorCurrency(vendorRequest.getCurrencyCode())
                .winAmount(vendorRequest.getAmount())
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}
