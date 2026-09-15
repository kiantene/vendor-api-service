package com.nextgen.gameaggregator.vendor.esoterica.api.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BetAndResultRequestMapper implements BetResultContextMapper<BetAndResultRequest> {
    @Override
    public BetResultContext toInternal(BetAndResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getBet().getReference())
                .vendorBetId(request.getBet().getReference())
                .roundId(String.valueOf(request.getBet().getRoundId()))
                .vendorGameCode(request.getBet().getGameName())
                .vendorPlayerUsername(request.getBet().getUserId())
                .betAmount(request.getBet().getAmount().divide(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE), 4, RoundingMode.DOWN))
                .winAmount(request.getWin().getAmount().divide(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE), 4, RoundingMode.DOWN))
                .vendorBetTime(request.getBet().getTimestamp())
                .vendorSettleTime(request.getWin().getTimestamp())
                .build();
    }
}
