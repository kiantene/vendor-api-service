package com.nextgen.gameaggregator.vendor.spribe.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getProviderTxId())
                .vendorBetId(request.getRollbackProviderTxId())
                .roundId(request.getActionId())
                .vendorPlayerUsername(request.getUserId())
                .vendorGameCode(request.getGame())
                .build();
    }
}
