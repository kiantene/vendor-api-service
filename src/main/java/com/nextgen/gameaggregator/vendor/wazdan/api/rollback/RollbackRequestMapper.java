package com.nextgen.gameaggregator.vendor.wazdan.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {
    @Override
    public BetRollbackContext toInternal(RollbackRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorBetId(request.getOriginalTransactionId())
                .token(request.getUser().getToken())
                .vendorPlayerUsername(request.getUser().getId())
                .build();
    }
}
