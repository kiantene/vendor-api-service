package com.nextgen.gameaggregator.vendor.lucky365.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.lucky365.util.LoginIds;
import com.nextgen.gameaggregator.vendor.lucky365.util.TimeStamp;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getId())
                .vendorBetId(request.getOrderCode())
                .roundId(request.getOrderCode())
                .vendorGameCode(request.getGameCode())
                .vendorPlayerUsername(LoginIds.forLookup(request.getLoginId()))
                .betAmount(request.getTotalBet())
                .timestamp(TimeStamp.convertTimeStamp(request.getActionDate()))
                .build();
    }
}