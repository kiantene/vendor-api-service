package com.nextgen.gameaggregator.vendor.gpkv2.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.gpkv2.util.TimeStampUtils;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorBetId(request.getTransactionId())
                .token(request.getSessionToken())
                .vendorPlayerUsername(request.getOperatorPlayerId())
                .vendorGameCode(request.getGameToken())
                .roundId(request.getRoundId())
                .betAmount(request.getAmount())
                .timestamp((TimeStampUtils.normalizeToMillis(request.getTimestamp())))
                .build();
    }
}