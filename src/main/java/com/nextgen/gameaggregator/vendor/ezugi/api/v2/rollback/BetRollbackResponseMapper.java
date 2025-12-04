package com.nextgen.gameaggregator.vendor.ezugi.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;

import java.math.BigInteger;

import org.springframework.stereotype.Component;

@Component
public class BetRollbackResponseMapper implements BetRollbackVendorResponseMapper<BetRollbackResponse> {
    @Override
    public BetRollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return BetRollbackResponse.builder()
                .token(context.getVendorSessionToken())
                .transactionId(context.getIdempotencyKey())
                .roundId(new BigInteger(context.getRoundId()))
                .uid(balanceData.getUsername())
                .currency(balanceData.getCurrency())
                .balance(balanceData.getBalance())
                .timestamp(balanceData.getTimestamp())
                .build();
    }
}
