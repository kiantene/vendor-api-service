package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionrollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class SessionRollbackRequestMapper implements BetRollbackContextMapper<SessionRollbackRequest> {
    @Override

    public BetRollbackContext toInternal(SessionRollbackRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(String.valueOf(request.getRound()))
                .vendorSessionToken(request.getToken())
                .token(request.getToken())
                .vendorPlayerUsername(request.getUserId())
                .vendorBetId(String.valueOf(request.getRound())) //Based on API Document, Round act as VendorBetId
                .roundId(String.valueOf(request.getSessionId())) //Based on API Document, SessionId act as roundId
                .vendorGameCode(String.valueOf(request.getGame()))
                .build();
    }
}
