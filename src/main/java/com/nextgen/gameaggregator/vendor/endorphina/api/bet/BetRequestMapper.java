package com.nextgen.gameaggregator.vendor.endorphina.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getId())
                .roundId(request.getId())
                .token(request.getToken())
                .betAmount(request.getAmount())
                .vendorCurrency(request.getCurrency())
                .vendorGameCode(request.getGame())
                .vendorPlayerUsername(request.getPlayer())
                .build();
    }
}
