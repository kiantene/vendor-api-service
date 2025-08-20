package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTransactionId())
                .roundId(request.getRoundId())
                .gameCode(request.getGameCode())
                .vendorPlayerUsername(request.getPlayerId())
                .vendorCurrency(request.getCurrencyCode())
                .betAmount(request.getAmount())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
