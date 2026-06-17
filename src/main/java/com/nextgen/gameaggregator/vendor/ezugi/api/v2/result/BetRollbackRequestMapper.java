package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRollbackRequestMapper implements BetRollbackContextMapper<BetResultRequest> {
    @Override
    public BetRollbackContext toInternal(BetResultRequest request) {
        return BetRollbackContext.builder()
                .idempotencyKey(request.getTransactionId())
                .roundId(request.getDebitTransactionId())
                .vendorGameCode(String.valueOf(request.getTableId()))
                .vendorPlayerUsername(request.getUid())
                .vendorCurrency(request.getCurrency())
                .vendorBetId(request.getDebitTransactionId())
                .vendorSessionToken(request.getToken())
                .timestamp(request.getTimestamp())
                .build();
    }
}