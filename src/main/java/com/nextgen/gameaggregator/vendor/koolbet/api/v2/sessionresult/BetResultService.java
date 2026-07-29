package com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionresult;

import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.koolbet.api.v2.sessionbet.SessionBetAndResultRequest;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BetResultService extends AbstractBetResultController<SessionBetAndResultRequest, CommonResponse> {

    protected BetResultService(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletBetResultService) {
        super(requestMapper, responseMapper, walletBetResultService);
    }

    public ResponseEntity<CommonResponse> result(SessionBetAndResultRequest request) {
        return ResponseEntity.ok(processRequest(request, (context, resp) -> enrichResponse(resp, request)));
    }

    @Override
    public void configure(BetResultConfig config, SessionBetAndResultRequest request) {
        SettleType settleType;
        if (request.getBetOrder() != null && request.getBetOrder().size() > 1) {
            settleType = SettleType.ROUND;
        } else {
            settleType = SettleType.BET;
        }
        config.betAndResult(false).setSettleType(settleType);
    }

    private void enrichResponse(CommonResponse response, SessionBetAndResultRequest request) {
        response.setCurrency(request.getCurrency());
    }
}
