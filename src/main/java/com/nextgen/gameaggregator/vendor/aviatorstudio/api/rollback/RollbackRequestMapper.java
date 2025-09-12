package com.nextgen.gameaggregator.vendor.aviatorstudio.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.result.BetResultRequest;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<BetResultRequest> {
    @Override
    public BetRollbackContext toInternal(BetResultRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorGameCode(request.getGameId())
                .vendorPlayerUsername(request.getUsername())
                .vendorBetId(request.getPreviousTransactionId())
                .vendorSessionToken(request.getSessionId())
                .token(request.getToken())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
