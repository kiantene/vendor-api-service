package com.nextgen.gameaggregator.vendor.digitain.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {

    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {

        String vendorBetId = vendorRequest.getOtxid() == null
                ? vendorRequest.getRid()
                : vendorRequest.getOtxid();

        return BetRollbackContext.builder()
                .idempotencyKey(vendorBetId)
                .vendorBetId(vendorBetId)
                .roundId(vendorRequest.getRid())
                .vendorPlayerUsername(vendorRequest.getPid())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
