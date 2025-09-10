package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public abstract class AbstractBetRollbackController<Q, R> extends AbstractProcessorController<Q, R, BetRollbackContext> {
    protected final WalletRollbackServiceWrapper walletService;

    protected AbstractBetRollbackController(BetRollbackContextMapper<Q> requestMapper,
                                            BetRollbackVendorResponseMapper<R> responseMapper,
                                            WalletRollbackServiceWrapper walletService) {
        super(requestMapper, responseMapper);
        this.walletService = walletService;
    }

    @Override
    protected PlayerBalanceData executeService(BetRollbackContext context, Q request) {
        return walletService
                .initialise(context)
                .configure(config -> configure(config, request))
                .process();
    }

    protected void configure(BetRollbackConfig config, Q request) {
        // override for config
    }
}
