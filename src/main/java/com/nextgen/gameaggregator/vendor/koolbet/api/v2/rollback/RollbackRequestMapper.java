package com.nextgen.gameaggregator.vendor.koolbet.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {
    @Override

    public BetRollbackContext toInternal(RollbackRequest request) {
        return BetRollbackContext.builder()
                .vendorSessionToken(request.getToken())
                .token(request.getToken())
                .idempotencyKey(String.valueOf(request.getRound()))
                .vendorPlayerUsername(request.getUserId())
                .vendorBetId(String.valueOf(request.getRound()))
                .vendorGameCode(String.valueOf(request.getGame()))
                .build();
    }
}
