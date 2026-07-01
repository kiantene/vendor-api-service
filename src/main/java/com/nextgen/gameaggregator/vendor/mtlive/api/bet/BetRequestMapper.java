package com.nextgen.gameaggregator.vendor.mtlive.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getBet_sn())
                .roundId(request.getBet_sn())
                .vendorPlayerUsername(request.getUser_id())
                .betAmount(request.getOrder_money().setScale(2, RoundingMode.DOWN))
                .build();
    }
}