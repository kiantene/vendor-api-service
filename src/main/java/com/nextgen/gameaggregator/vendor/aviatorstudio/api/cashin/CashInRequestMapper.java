package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class CashInRequestMapper implements BetResultContextMapper<CashInRequest> {
    @Override
    public BetResultContext toBetResultContext(CashInRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionId())
//                .vendorPlayerUsername() // get from JWT
                .gameCode(request.getGameId())
                .roundId(request.getRoundId())
                .vendorBetId(request.getTransactionId())
                .vendorCurrency(request.getCurrency())
                .vendorSessionToken(request.getSessionId())
                .betAmount(request.getAmount())
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}
