package com.nextgen.gameaggregator.vendor.ezugi.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRollbackRequestMapper implements BetRollbackContextMapper<BetRollbackRequest> {
    @Override
    public BetRollbackContext toInternal(BetRollbackRequest vendorRequest) {
        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorBetId(vendorRequest.getTransactionId())
                .roundId(vendorRequest.getRoundId())
                .vendorPlayerUsername(vendorRequest.getUid())
                .vendorSessionToken(vendorRequest.getToken())
                .build();
    }
}
