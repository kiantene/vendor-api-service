package com.nextgen.gameaggregator.vendor.gpkv2.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {

        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorBetId(vendorRequest.getTransactionId())
                .vendorPlayerUsername(vendorRequest.getOperatorPlayerId())
                .roundId(vendorRequest.getRoundId())
                .build();
    }
}