package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInRequest;
import org.springframework.stereotype.Component;

@Component
class RollbackRequestMapper implements BetRollbackContextMapper<CashInRequest> {
    @Override
    public BetRollbackContext toBetRollbackContext(CashInRequest vendorRequest) {
        return BetRollbackContext.builder()
                .rollbackType(RollbackType.BY_BET)
                .idempotencyKey(vendorRequest.getTransactionId())
//                .vendorPlayerUsername() // get from JWT
                .roundId(vendorRequest.getRoundId())
                .vendorBetId(vendorRequest.getPreviousTransactionId())
                .vendorSessionToken(vendorRequest.getSessionId())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
