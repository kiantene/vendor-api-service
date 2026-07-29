package com.nextgen.gameaggregator.vendor.cockfight6.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<CommonRequest> {
    @Override
    public BetContext toInternal(CommonRequest request) {
        return BetContext.builder()
                .idempotencyKey(String.valueOf(request.getRecordId()))
                .vendorBetId(String.valueOf(request.getRecordId()))
                .roundId(request.getBet().getGameRoundId())
                .vendorPlayerUsername(request.getPlayerName())
                .betAmount(request.getChange().abs())
                .timestamp(request.getCreateTime() * 1000)
                .build();
    }
}
