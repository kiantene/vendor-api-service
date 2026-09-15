package com.nextgen.gameaggregator.vendor.esoterica.api.result.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import com.nextgen.gameaggregator.vendor.esoterica.request.CommonRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("betAndResultServiceRequestMapperForResult")
public class BetAndResultServiceRequestMapper implements BetResultContextMapper<CommonRequest> {
    @Override
    public BetResultContext toInternal(CommonRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getReference())
                .vendorGameCode(request.getGameName())
                .roundId(String.valueOf(request.getRoundId()))
                .betAmount(BigDecimal.ZERO)
                .winAmount(request.getAmount().divide(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE), 4, RoundingMode.DOWN))
                .vendorPlayerUsername(request.getUserId())
                .vendorSettleTime(request.getTimestamp())
                .isFreeSpin(request.getGameName().equals("vikingGold") ? 1 : 0)
                .build();
    }
}
