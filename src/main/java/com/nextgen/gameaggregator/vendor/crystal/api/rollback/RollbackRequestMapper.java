package com.nextgen.gameaggregator.vendor.crystal.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {
        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorBetId(vendorRequest.getTransactionOriginalId())
                .roundId(vendorRequest.getRoundId())
                .vendorPlayerUsername(vendorRequest.getPlayerId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
