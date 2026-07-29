package com.nextgen.gameaggregator.vendor.topbet.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import org.springframework.stereotype.Component;

import static com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil.formatTimestamp;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTransId())
                .roundId(request.getActionId())
                .vendorGameCode(String.valueOf(request.getAppId()))
                .vendorPlayerUsername(request.getAccount())
                .betAmount(request.getAmount())
                .timestamp(formatTimestamp(request.getTime()))
                .build();
    }
}