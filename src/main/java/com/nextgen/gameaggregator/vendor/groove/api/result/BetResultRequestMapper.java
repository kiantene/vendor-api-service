package com.nextgen.gameaggregator.vendor.groove.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionid())
                .token(VendorUtil.extractTokenFromSessionId(request.getGamesessionid()))
                .vendorPlayerUsername(request.getAccountid())
                .gameCode(request.getGameid())
                .roundId(request.getRoundid())
                .winAmount(request.getResult())
                .roundEnded(request.getGamestatus().equals("completed"))
                .build();
    }
}