package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public abstract class AbstractBetController<Q, R> extends AbstractProcessorController<Q, R, BetContext> {

    protected final WalletBetService walletService;

    protected AbstractBetController(BetContextMapper<Q> requestMapper,
                                    BetVendorResponseMapper<R> responseMapper,
                                    WalletBetService walletService) {
        super(requestMapper, responseMapper);
        this.walletService = walletService;
    }

    @Override
    protected PlayerBalanceData executeService(BetContext context, Q request) {
        return walletService
                .initialise(context)
                .configure(config -> configure(config, request))
                .process();
    }

    protected void configure(BetConfig config, Q request) {
        // override for config
    }
}
