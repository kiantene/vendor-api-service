package com.nextgen.gameaggregator.vendor.aviatorstudio.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.result.BetResultRequest;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<BetResultRequest> {
    @Override
    public BetRollbackContext toInternal(BetResultRequest vendorRequest) {
        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorPlayerUsername(vendorRequest.getUsername())
                .vendorBetId(vendorRequest.getPreviousTransactionId())
                .vendorSessionToken(vendorRequest.getSessionId())
                .token(vendorRequest.getToken())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
