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
                .vendorBetId(request.getPreviousTransactionId())
                .roundId(request.getRoundId())
                .gameCode(request.getGameId())
//                .vendorPlayerUsername() // get from JWT
                .vendorCurrency(request.getCurrency())
                .winAmount(request.getAmount())
                .vendorSessionToken(request.getSessionId())
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}
