package com.nextgen.gameaggregator.vendor.vplus.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.vplus.api.result.BetResultRequest;
import org.springframework.stereotype.Component;

@Component
class RollbackRequestMapper implements BetRollbackContextMapper<BetResultRequest> {
    @Override

    public BetRollbackContext toInternal(BetResultRequest vendorRequest) {
        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorBetId(vendorRequest.getTransactionId())
                .roundId(vendorRequest.getGameRoundId())
                .vendorPlayerUsername(vendorRequest.getUsername())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
