package com.nextgen.gameaggregator.vendor.megafair.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getBetTransactionId())
                .vendorSessionToken(request.getGameSessionId())
                .vendorPlayerUsername(request.getUserId())
                .vendorGameCode(request.getGameId())
                .roundId(request.getRoundId())
                .betAmount(request.getBetAmount())
                .build();
    }
}
