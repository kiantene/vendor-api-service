package com.nextgen.gameaggregator.vendor.evoplay.api.v2.bet;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;

@Component
public class BetRequestMapper implements BetContextMapper<CallbackDto> {

    @Override
    public BetContext toInternal(CallbackDto request) {
        return BetContext.builder()
                .idempotencyKey(request.getCallback_id())
                .token(request.getToken())
                .vendorBetId(request.getData().getAction_id())
                .vendorGameCode(request.getData().getDetailsDto().getGame().getGame_id())
                .vendorPlayerUsername(request.getUsername())
                .roundId(request.getData().getRound_id())
                .betAmount(new BigDecimal(request.getData().getAmount()))
                .build();
    }
}
