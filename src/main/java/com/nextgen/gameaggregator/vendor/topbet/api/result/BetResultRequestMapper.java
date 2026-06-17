package com.nextgen.gameaggregator.vendor.topbet.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import org.springframework.stereotype.Component;

import static com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil.formatTimestamp;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransId())
                .roundId(request.getActionId())
                .vendorGameCode(String.valueOf(request.getAppId()))
                .vendorPlayerUsername(request.getAccount())
                .winAmount(request.getAmount())
                .vendorSettleTime(formatTimestamp(request.getSettleTime()))
                .roundEnded(true)
                .build();
    }
}
