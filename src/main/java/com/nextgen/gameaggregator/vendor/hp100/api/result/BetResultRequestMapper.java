package com.nextgen.gameaggregator.vendor.hp100.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.hp100.util.StrictBigDecimalConverter;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {

    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTxId())
                .token(request.getSessionId())
                .vendorPlayerUsername(request.getUserId())
                .roundId(request.getTxId())
                .winAmount(StrictBigDecimalConverter.getAmountAsBigDecimal(request.getAmount()))
                .roundEnded(true)
                .build();

    }
}
