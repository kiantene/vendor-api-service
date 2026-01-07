package com.nextgen.gameaggregator.vendor.endorphina.api.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetAndResultRequestMapper implements BetResultContextMapper<BetAndResultRequest> {
    @Override
    public BetResultContext toInternal(BetAndResultRequest request) {

        return BetResultContext.builder()
                .idempotencyKey(request.getId())
                .roundId(request.getId())
                .vendorGameCode(request.getGame() != null ? request.getGame() : null)
                .vendorPlayerUsername(request.getPlayer())
                .betAmount(BigDecimal.ZERO)
                .vendorBetTime(System.currentTimeMillis())
                .vendorSettleTime(System.currentTimeMillis())
                .vendorCurrency(request.getCurrency())
                .winAmount(BigDecimal.ZERO)
                .jackpotAmount(request.getAmount())
                .build();
    }
}