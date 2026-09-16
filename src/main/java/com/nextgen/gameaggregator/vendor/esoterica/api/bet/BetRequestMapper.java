package com.nextgen.gameaggregator.vendor.esoterica.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.StringConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {

        String uniqueId = request.getUserId() + request.getGameName() + request.getRoundId();

        return BetContext.builder()
                .idempotencyKey(request.getReference())
                .roundId(uniqueId)
                .betAmount(request.getAmount().divide(BigDecimal.valueOf(StringConstants.VENDOR_AMOUNT_SCALE), 4, RoundingMode.DOWN))
                .vendorPlayerUsername(request.getUserId())
                .vendorGameCode(request.getGameName())
                .timestamp(request.getTimestamp())
                .build();
    }
}
