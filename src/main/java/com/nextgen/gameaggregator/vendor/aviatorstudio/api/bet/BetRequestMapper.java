package com.nextgen.gameaggregator.vendor.aviatorstudio.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTransactionId())
//                .vendorPlayerUsername() // get from JWT
                .gameCode(request.getGameId())
                .roundId(request.getRoundId())
                .vendorBetId(request.getTransactionId())
                .vendorCurrency(request.getCurrency())
                .vendorSessionToken(request.getSessionId())
                .betAmount(request.getAmount())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
