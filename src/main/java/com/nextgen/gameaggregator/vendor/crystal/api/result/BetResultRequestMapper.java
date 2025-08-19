package com.nextgen.gameaggregator.vendor.crystal.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorPlayerUsername(request.getPlayerId())
                .gameCode(request.getGameCode())
                .roundId(request.getRoundId())
                .vendorBetId(request.getTransactionId())
                .winAmount(request.getAmount())
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}