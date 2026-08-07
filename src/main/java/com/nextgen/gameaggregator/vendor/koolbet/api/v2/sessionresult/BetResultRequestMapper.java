package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet.SessionBetAndResultRequest;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<SessionBetAndResultRequest> {
    @Override
    public BetResultContext toInternal(SessionBetAndResultRequest request) {
        // Match v1 (SessionBetNSettleDto#getExternalTransactionId / #getVendorBetId):
        // a single betOrder → settle by that bet id; a batch settle (size > 1) or an
        // absent betOrder (result0 / duplicate flows) → key on round. The size > 1 branch
        // is intentional v1 parity, not an oversight; the null/empty guard also prevents
        // the NPE that the previous betOrder.get(0) hit on betOrder-less settles.
        String externalTransactionId =
                request.getBetOrder() != null && request.getBetOrder().size() == 1
                        ? request.getBetOrder().get(0)
                        : String.valueOf(request.getRound());
        return BetResultContext.builder()
                .idempotencyKey(externalTransactionId) // drives operator external_transaction_id + duplicate detection
                .roundId(String.valueOf(request.getSessionId())) //Based on API Document, SessionId act as roundId
                .vendorGameCode(String.valueOf(request.getGame()))
                .vendorBetId(externalTransactionId) // Settle by bet when betOrder size is 1
                .winAmount(request.getWinloseAmount())
                .token(request.getToken())
                .vendorPlayerUsername(request.getUsername())
                .roundEnded(request.getIsOver())
                //.roundEnded(false)
                //set wagers time to settle time
                .vendorSettleTime(request.getWagersTime() * 1000)
                .build();
    }

}