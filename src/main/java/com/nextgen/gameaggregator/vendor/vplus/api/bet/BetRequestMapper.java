package com.nextgen.gameaggregator.vendor.vplus.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTransactionId())
                .roundId(request.getGameRoundId())
                .vendorGameCode(String.valueOf(request.getGameId()))
                .vendorPlayerUsername(request.getUsername())
                .betAmount(request.getBalance())
                .timestamp(request.getTimestamp())
                .build();
    }
}