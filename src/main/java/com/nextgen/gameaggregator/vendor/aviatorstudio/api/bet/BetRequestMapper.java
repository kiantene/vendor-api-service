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
                .roundId(request.getRoundId())
                .gameCode(request.getGameId())
//                .vendorPlayerUsername() // get from JWT
                .vendorCurrency(request.getCurrency())
                .betAmount(request.getAmount())
                .vendorSessionToken(request.getSessionId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
