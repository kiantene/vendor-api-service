package com.nextgen.gameaggregator.vendor.koolbet.api.v2.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetAndResultRequestMapper implements BetResultContextMapper<BetAndResultRequest> {
    @Override
    public BetResultContext toInternal(BetAndResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(String.valueOf(request.getRound()))
                .token(request.getToken())
                .roundId(String.valueOf(request.getRound()))
                .vendorGameCode(String.valueOf(request.getGame()))
                .vendorBetId(String.valueOf(request.getRound()))
                .betAmount(request.getBetAmount())
                .winAmount(request.getWinloseAmount())
                .vendorPlayerUsername(request.getUsername())
                .roundEnded(true)
                .build();
    }
}
