package com.nextgen.gameaggregator.vendor.hp100.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.hp100.util.StrictBigDecimalConverter;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    GameSessionService sessionService = null;

    @Override
    public BetContext toInternal(BetRequest request) {

        return BetContext.builder()
                .idempotencyKey(request.getTxId())
                .roundId(request.getTxId())
                .vendorPlayerUsername(request.getUserId())
                .token(request.getSessionId())
                .betAmount(StrictBigDecimalConverter.getAmountAsBigDecimal(request.getAmount()))
                .build();
    }

}
