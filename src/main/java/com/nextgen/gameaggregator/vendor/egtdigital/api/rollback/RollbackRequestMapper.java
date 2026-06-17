package com.nextgen.gameaggregator.vendor.egtdigital.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {
        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getRoundNumber())
                .vendorBetId(vendorRequest.getTransferId())
                .roundId(vendorRequest.getRoundNumber())
                .vendorSessionToken(vendorRequest.getSessionId())
                .vendorPlayerUsername(vendorRequest.getPlayerId())
                .build();
    }
}
