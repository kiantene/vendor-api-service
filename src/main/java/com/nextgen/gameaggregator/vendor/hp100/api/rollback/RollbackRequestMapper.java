package com.nextgen.gameaggregator.vendor.hp100.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getTxId())
                .token(request.getSessionId())
                .vendorPlayerUsername(request.getUserId())
                .vendorBetId(request.getTxId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
