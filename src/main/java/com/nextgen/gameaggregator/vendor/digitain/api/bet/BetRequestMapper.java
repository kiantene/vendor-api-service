package com.nextgen.gameaggregator.vendor.digitain.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTxid())
                .roundId(request.getRid())
                .vendorGameCode(request.getGid())
                .token(request.getTkn())
                .vendorPlayerUsername(request.getPid())
                .vendorCurrency(request.getCid())
                .betAmount(request.getInfo().isIsb() ? BigDecimal.ZERO : request.getBam())
                .build();
    }
}
