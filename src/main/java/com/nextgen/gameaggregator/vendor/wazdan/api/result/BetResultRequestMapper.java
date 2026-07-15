package com.nextgen.gameaggregator.vendor.wazdan.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
                .vendorBetId(request.getRound().getBetTransactionId())
                .roundId(request.getRoundId())
                .vendorGameCode(String.valueOf(request.getGameId()))
                .vendorPlayerUsername(request.getUser().getId())
                .token(request.getUser().getToken())
                .winAmount(request.getAmount())
                .roundEnded(request.getRound().getEndRound())
                .vendorBetTime(System.currentTimeMillis())
                .build();
    }
}
