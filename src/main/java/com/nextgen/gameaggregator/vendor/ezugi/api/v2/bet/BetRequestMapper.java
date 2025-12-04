package com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest vendorRequest) {
        return BetContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorSessionToken(vendorRequest.getToken())
                .vendorPlayerUsername(vendorRequest.getUid())
                .vendorBetId(vendorRequest.getTransactionId())
                .roundId(vendorRequest.getTransactionId())
                .vendorGameCode(vendorRequest.getTableId().toString())
                .vendorCurrency(vendorRequest.getCurrency())
                .betAmount(vendorRequest.getDebitAmount())
                .timestamp(vendorRequest.getTimestamp())
                .build();
    }
}
