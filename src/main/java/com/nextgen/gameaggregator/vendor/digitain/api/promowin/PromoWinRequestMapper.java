package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PromoWinRequestMapper implements BetResultContextMapper<PromoWinRequest> {
    @Override
    public BetResultContext toInternal(PromoWinRequest request) {

        return BetResultContext.builder()
                .idempotencyKey(request.getTxid())
                .roundId(request.getTxid())
                .vendorGameCode(request.getGid())
                .vendorPlayerUsername(request.getPid())
                .betAmount(BigDecimal.ZERO)
                .winAmount(BigDecimal.ZERO)
                .jackpotAmount(request.getPwa())
                .vendorBetTime(System.currentTimeMillis())
                .vendorSettleTime(System.currentTimeMillis())
                .build();
    }
}
