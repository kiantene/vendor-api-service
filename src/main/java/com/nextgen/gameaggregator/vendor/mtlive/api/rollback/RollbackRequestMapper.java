package com.nextgen.gameaggregator.vendor.mtlive.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {
    @Override

    public BetRollbackContext toInternal(RollbackRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getBet_sn())
                .vendorBetId(request.getBet_sn())
                .vendorPlayerUsername(request.getUser_id())
                .build();
    }
}
