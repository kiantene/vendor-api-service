package com.nextgen.gameaggregator.vendor.groove.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContextMapper;
import com.nextgen.gameaggregator.vendor.groove.util.VendorUtil;
import org.springframework.stereotype.Component;

@Component
public class BetRequestMapper implements BetContextMapper<BetRequest> {
    @Override
    public BetContext toInternal(BetRequest request) {
        return BetContext.builder()
                .idempotencyKey(request.getTransactionid())
                .token(VendorUtil.extractTokenFromSessionId(request.getGamesessionid()))
                .vendorPlayerUsername(request.getAccountid())
                .gameCode(request.getGameid())
                .roundId(request.getRoundid())
                .betAmount(request.getBetamount())
                .build();
    }
}