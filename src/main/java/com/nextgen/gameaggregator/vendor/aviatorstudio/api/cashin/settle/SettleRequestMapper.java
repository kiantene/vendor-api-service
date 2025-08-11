package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.settle;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInRequest;
import org.springframework.stereotype.Component;

@Component
class SettleRequestMapper implements BetResultContextMapper<CashInRequest> {
    @Override
    public BetResultContext toBetResultContext(CashInRequest request) {
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
