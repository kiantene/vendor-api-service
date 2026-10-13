package com.nextgen.gameaggregator.vendor.megafair.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getWinTransactionId())
                .vendorSessionToken(request.getGameSessionId())
                .vendorPlayerUsername(request.getUserId())
                .vendorGameCode(request.getGameId())
                .roundId(request.getRoundId())
                .winAmount(request.getWinAmount())
                .roundEnded(isTrue(request.getIsRoundFinished()))
                .build();
    }

    private boolean isTrue(String val) {
        return "1".equals(val) || "true".equalsIgnoreCase(val);
    }
}
