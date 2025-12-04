package com.nextgen.gameaggregator.vendor.ezugi.api.v2.tip;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet.BetRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TipRequestMapper implements BetResultContextMapper<BetRequest> {
    @Override
    public BetResultContext toInternal(BetRequest vendorRequest) {
        return BetResultContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .token(vendorRequest.getToken())
                .vendorPlayerUsername(vendorRequest.getUid())
                .vendorBetId(vendorRequest.getTransactionId())
                .roundId(vendorRequest.getTransactionId())
                .vendorGameCode(vendorRequest.getGameId().toString())
                .currencyCode(vendorRequest.getCurrency())
                .betAmount(vendorRequest.getDebitAmount())
                .winAmount(BigDecimal.ZERO)
                .vendorBetTime(vendorRequest.getTimestamp())
                .vendorSettleTime(vendorRequest.getTimestamp())
                .build();
    }
}
