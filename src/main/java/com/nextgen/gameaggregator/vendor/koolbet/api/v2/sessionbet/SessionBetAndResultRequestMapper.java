package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
public class SessionBetAndResultRequestMapper implements BetContextMapper<SessionBetAndResultRequest> {
    @Override
    public BetContext toInternal(SessionBetAndResultRequest request) {
        return BetContext.builder()
                .idempotencyKey(String.valueOf(request.getRound()))
                .vendorBetId(String.valueOf(request.getRound())) // Bet Order is null during bet, so using Round as VendorBetId
                .roundId(String.valueOf(request.getSessionId())) //Based on API Document, SessionId act as roundId
                .vendorGameCode(String.valueOf(request.getGame()))
                .betAmount(request.getBetAmount())
                .token(request.getToken())
                .vendorPlayerUsername(request.getUsername())
                .build();
    }
}
