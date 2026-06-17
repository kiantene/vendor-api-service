package com.nextgen.gameaggregator.vendor.egtdigital.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {

        return BetContext.builder()
                .idempotencyKey(request.getRoundNumber())
                .vendorBetId(request.getTransferId())
                .roundId(request.getRoundNumber())
                .vendorGameCode(request.getGameKey())
                .vendorSessionToken(request.getSessionId())
                .vendorPlayerUsername(request.getPlayerId())
                .vendorCurrency(request.getCurrency())
                .betAmount(request.getReason().equals("GIFT_SPIN")? BigDecimal.ZERO: Amount.internal(request.getAmount()))
                .build();
    }
}
