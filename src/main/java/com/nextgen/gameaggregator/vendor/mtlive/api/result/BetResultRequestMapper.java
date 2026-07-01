package com.nextgen.gameaggregator.vendor.mtlive.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getBet_sn())
                .vendorBetId(request.getBet_sn())
                .roundId(request.getBet_sn())
                .vendorPlayerUsername(request.getUser_id())
                .winAmount(request.getWin_money())
                .build();
    }
}
