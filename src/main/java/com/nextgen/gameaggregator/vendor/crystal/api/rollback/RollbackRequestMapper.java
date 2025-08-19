package com.nextgen.gameaggregator.vendor.crystal.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {
        return BetRollbackContext.builder()
                .rollbackType(RollbackType.BY_ROUND)
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorPlayerUsername(vendorRequest.getPlayerId())
                .roundId(vendorRequest.getRoundId())
                .vendorBetId(vendorRequest.getRoundId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}