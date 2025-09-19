package com.nextgen.gameaggregator.vendor.crystal.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {
        String vendorBetId = vendorRequest.getTransactionOriginalId() == null
                ? vendorRequest.getTransactionId()
                : vendorRequest.getTransactionOriginalId();

        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorBetId(vendorBetId)
                .roundId(vendorRequest.getRoundId())
                .vendorPlayerUsername(vendorRequest.getPlayerId())
                .build();
    }
}
