package com.nextgen.gameaggregator.vendor.ezugi.api.v2.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRollbackResponseMapper implements BetRollbackVendorResponseMapper<BetResultResponse> {
    @Override
    public BetResultResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return BetResultResponse.builder()
                .token(context.getVendorSessionToken())
                .transactionId(context.getIdempotencyKey())
                .uid(balanceData.getUsername())
                .currency(balanceData.getCurrency())
                .balance(balanceData.getBalance())
                .timestamp(balanceData.getTimestamp())
                .build();
    }
}