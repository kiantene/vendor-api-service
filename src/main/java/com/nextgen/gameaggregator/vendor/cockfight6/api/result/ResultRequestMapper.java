package com.nextgen.gameaggregator.vendor.cockfight6.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.cockfight6.request.CommonRequest;
import org.springframework.stereotype.Component;

@Component
public class ResultRequestMapper implements BetResultContextMapper<CommonRequest> {
    @Override
    public BetResultContext toInternal(CommonRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(String.valueOf(request.getRecordId()))
                .vendorPlayerUsername(request.getPlayerName())
                .roundId(request.getSettle().getGameRoundId())
                .winAmount(request.getChange())
                .vendorSettleTime(request.getCreateTime() * 1000)
                .roundEnded(true)//Confirmed with vendor only one settlement request.
                .build();
    }
}
