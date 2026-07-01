package com.nextgen.gameaggregator.vendor.mtlive.api.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetAndResultRequestMapper implements BetResultContextMapper<BetAndResultRequest> {
    @Override
    public BetResultContext toInternal(BetAndResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTip_sn())
                .vendorBetId(request.getTip_sn())
                .roundId(request.getTip_sn())
                .vendorPlayerUsername(request.getUser_id())
                .betAmount(request.getMoney())
                .winAmount(BigDecimal.ZERO)
                .build();
    }
}
