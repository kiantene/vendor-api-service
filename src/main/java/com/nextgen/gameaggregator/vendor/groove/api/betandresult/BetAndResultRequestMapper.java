package com.nextgen.gameaggregator.vendor.groove.api.betandresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import org.springframework.stereotype.Component;

@Component
public class BetAndResultRequestMapper implements BetResultContextMapper<BetAndResultRequest> {
    @Override
    public BetResultContext toInternal(BetAndResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getTransactionid())
                .token(VendorUtil.extractTokenFromSessionId(request.getGamesessionid()))
                .roundId(request.getRoundid())
                .gameCode(request.getGameid())
                .vendorPlayerUsername(request.getAccountid())
                .betAmount(request.getBetamount())
                .winAmount(request.getResult())
                .vendorBetTime(System.currentTimeMillis())
                .roundEnded(request.getGamestatus().equals("completed"))
                .build();
    }
}
