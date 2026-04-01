package com.nextgen.gameaggregator.vendor.digitain.api.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BetAndResultRequestMapper implements BetResultContextMapper<BetAndResultRequest> {
    @Override
    public BetResultContext toInternal(BetAndResultRequest request) {

        return BetResultContext.builder()
                .idempotencyKey(request.getTxid())
                .roundId(request.getRid())
                .vendorGameCode(request.getGid())
                .token(request.getTkn())
                .vendorPlayerUsername(request.getPid())
                .betAmount(request.getInfo().isIsb() ? BigDecimal.ZERO : request.getBam())
                .isFreeSpin(request.getInfo().getWot().equals(4) ? 1 : 0)
                .jackpotAmount(request.getInfo().isIsb() ? request.getWam() : BigDecimal.ZERO)
                .winAmount(request.getInfo().isIsb() ? BigDecimal.ZERO : request.getWam())
                .roundEnded(request.getInfo().isRndf())
                .vendorBetTime(System.currentTimeMillis())
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}
