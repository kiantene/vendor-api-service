package com.nextgen.gameaggregator.vendor.topbet.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

import static com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil.formatTimestamp;

@Component
public class RollbackRequestMapper implements BetRollbackContextMapper<RollbackRequest> {
    @Override
    public BetRollbackContext toInternal(RollbackRequest vendorRequest) {
        return BetRollbackContext.builder()
                .idempotencyKey(vendorRequest.getTransId())
                .vendorBetId(vendorRequest.getTransId())
                .vendorPlayerUsername(vendorRequest.getAccount())
                .timestamp(formatTimestamp(vendorRequest.getTime()))
                .build();
    }
}
