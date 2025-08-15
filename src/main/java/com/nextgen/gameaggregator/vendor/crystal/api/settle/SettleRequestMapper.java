package com.nextgen.gameaggregator.vendor.crystal.api.settle;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class SettleRequestMapper implements BetResultContextMapper<SettleRequest> {
    @Override
    public BetResultContext toBetResultContext(SettleRequest vendorRequest) {
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
