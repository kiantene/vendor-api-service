package com.nextgen.gameaggregator.vendor.esoterica.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {

        String uniqueId = request.getUserId() + request.getGameName() + request.getRoundId();

        return BetResultContext.builder()
                .idempotencyKey(request.getReference())
                .vendorGameCode(request.getGameName())
                .roundId(uniqueId)
                .winAmount(request.getAmount().divide(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE), 4, RoundingMode.DOWN))
                .vendorPlayerUsername(request.getUserId())
                .vendorSettleTime(request.getTimestamp())
                .roundEnded(true)
                .build();
    }
}
