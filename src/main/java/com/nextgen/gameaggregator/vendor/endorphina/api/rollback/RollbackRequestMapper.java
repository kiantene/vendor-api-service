package com.nextgen.gameaggregator.vendor.endorphina.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {

        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getId())
                .vendorBetId(vendorRequest.getId())
                .roundId(vendorRequest.getId())
                .vendorPlayerUsername(vendorRequest.getPlayer())
                .vendorGameCode(vendorRequest.getGame())
                .timestamp(vendorRequest.getDate())
                .build();
    }
}
