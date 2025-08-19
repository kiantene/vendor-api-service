package com.nextgen.gameaggregator.vendor.aviatorstudio.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
//                .vendorPlayerUsername() // get from JWT
                .gameCode(request.getGameId())
                .roundId(request.getRoundId())
                .vendorBetId(request.getPreviousTransactionId())
                .vendorCurrency(request.getCurrency())
                .vendorSessionToken(request.getSessionId())
                .winAmount(request.getAmount())
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}
