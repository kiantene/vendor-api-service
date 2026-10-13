package com.nextgen.gameaggregator.vendor.megafair.api.bet;

import com.nextgen.gameaggregator.core.engine.wallet.bet.AbstractBetController;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig;
import com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService;
import com.nextgen.gameaggregator.vendor.megafair.response.CommonResponse;
import org.springframework.stereotype.Service;

@Service
public class BetService extends AbstractBetController<BetRequest, CommonResponse> {
    protected BetService(BetRequestMapper requestMapper,
                         BetResponseMapper responseMapper,
                         WalletBetService walletBetService) {
        super(requestMapper, responseMapper, walletBetService);
    }

    public CommonResponse bet(BetRequest request) {
        return processRequest(request);
    }

    @Override
    protected void configure(BetConfig config, BetRequest request) {
        config.allowMultipleBet(false);
    }
}
