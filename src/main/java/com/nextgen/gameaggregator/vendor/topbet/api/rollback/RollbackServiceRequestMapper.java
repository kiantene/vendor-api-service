package com.nextgen.gameaggregator.vendor.topbet.api.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.topbet.api.result.BetResultRequest;
import org.springframework.stereotype.Component;

import static com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil.formatTimestamp;

@Component
public class RollbackServiceRequestMapper implements BetRollbackContextMapper<BetResultRequest> {
    @Override
    public BetRollbackContext toInternal(BetResultRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getTransId())
                .vendorBetId(request.getTransId())
                .roundId(request.getActionId())
                .vendorPlayerUsername(request.getAccount())
                .timestamp(formatTimestamp(request.getTime()))
                .build();
    }
}
