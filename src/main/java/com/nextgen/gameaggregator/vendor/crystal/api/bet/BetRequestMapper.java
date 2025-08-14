package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toBetContext(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTransactionId())
                .externalTransactionId(request.getTransactionId())
                .vendorPlayerUsername(request.getPlayerId())
                .gameCode(request.getGameCode())
                .roundId(request.getRoundId())
                .vendorBetId(request.getTransactionId())
                .betAmount(request.getAmount())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
