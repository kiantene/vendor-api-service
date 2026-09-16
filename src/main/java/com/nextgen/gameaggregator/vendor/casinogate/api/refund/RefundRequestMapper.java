package com.nextgen.gameaggregator.vendor.casinogate.api.refund;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.vendor.casinogate.util.CasinoGateUtils;
import org.springframework.stereotype.Component;

@Component
public class RefundRequestMapper implements BetRollbackContextMapper<RefundRequest> {
    @Override
    public BetRollbackContext toInternal(RefundRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getRefundTransactionId())
                .vendorBetId(request.getTransactionId())
                .roundId(request.getRoundId())
                .rollbackAmount(CasinoGateUtils.toMajorUnits(request.getAmount()))
                .vendorPlayerUsername(request.getVendorPlayerUsername())
                .token(request.getToken())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
