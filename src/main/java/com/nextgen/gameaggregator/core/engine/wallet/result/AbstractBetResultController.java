package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public abstract class AbstractBetResultController<Q, R> extends AbstractProcessorController<Q, R, BetResultContext> {
    protected final WalletBetResultServiceWrapper walletService;

    protected AbstractBetResultController(BetResultContextMapper<Q> requestMapper,
                                          BetResultVendorResponseMapper<R> responseMapper,
                                          WalletBetResultServiceWrapper walletService) {
        super(requestMapper, responseMapper);
        this.walletService = walletService;
    }

    @Override
    protected PlayerBalanceData executeService(BetResultContext context, Q request) {
        return walletService
                .initialise(context)
                .configure(config -> configure(config, request))
                .process();
    }

    protected void configure(BetResultConfig config, Q request) {
        // override for config
    }
}
