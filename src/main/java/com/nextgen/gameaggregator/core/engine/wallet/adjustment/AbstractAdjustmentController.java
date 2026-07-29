package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public abstract class AbstractAdjustmentController<Q, R> extends AbstractProcessorController<Q, R, AdjustmentContext> {
    protected final WalletAdjustmentServiceWrapper walletService;

    protected AbstractAdjustmentController(AdjustmentContextMapper<Q> requestMapper,
                                           AdjustmentVendorResponseMapper<R> responseMapper,
                                           WalletAdjustmentServiceWrapper walletService) {
        super(requestMapper, responseMapper);
        this.walletService = walletService;
    }

    @Override
    protected PlayerBalanceData executeService(AdjustmentContext context, Q request) {
        return walletService
                .initialise(context)
                .configure(config -> configure(config, request))
                .process();
    }

    protected void configure(AdjustmentConfig config, Q request) {
        // override for config
    }
}
