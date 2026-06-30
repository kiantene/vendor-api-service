package com.nextgen.gameaggregator.vendor.groove.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import org.springframework.stereotype.Component;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {
    @Override

    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {
        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransactionid())
                .token(VendorUtil.extractTokenFromSessionId(vendorRequest.getGamesessionid()))
                .vendorPlayerUsername(vendorRequest.getAccountid())
                .vendorBetId(vendorRequest.getTransactionid())
                .build();
    }
}