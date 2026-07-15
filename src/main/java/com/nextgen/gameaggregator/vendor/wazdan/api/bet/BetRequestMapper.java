package com.nextgen.gameaggregator.vendor.wazdan.api.bet;

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
                .vendorGameCode(String.valueOf(request.getGameId()))
                .token(request.getUser().getToken())
                .vendorPlayerUsername(request.getUser().getId())
                .betAmount(request.getAmount())
                .build();
    }
}