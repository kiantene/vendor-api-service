package com.nextgen.gameaggregator.vendor.megafair.api.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.AbstractBetResultController;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultConfig;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.result.enums.SettleType;
import com.nextgen.gameaggregator.vendor.megafair.response.CommonResponse;
import org.springframework.stereotype.Service;

@Service
public class BetResultService extends AbstractBetResultController<BetResultRequest, CommonResponse> {
    protected BetResultService(BetResultRequestMapper requestMapper,
                               BetResultResponseMapper responseMapper,
                               WalletBetResultServiceWrapper walletBetResultService) {
        super(requestMapper, responseMapper, walletBetResultService);
    }

    public CommonResponse result(BetResultRequest request) {
        return processRequest(request);
    }

    @Override
    public void configure(BetResultConfig config, BetResultRequest request) {
        config.allowResultWhenRoundHasEnded(false).setSettleType(SettleType.ROUND);
    }
}
