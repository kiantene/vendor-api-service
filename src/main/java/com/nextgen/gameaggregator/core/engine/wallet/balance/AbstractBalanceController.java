package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.common.AbstractProcessorController;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public abstract class AbstractBalanceController<Q, R> extends AbstractProcessorController<Q, R, BalanceContext> {
    protected final WalletBalanceService walletService;

    protected AbstractBalanceController(BalanceContextMapper<Q> requestMapper,
                                        BalanceVendorResponseMapper<R> responseMapper,
                                        WalletBalanceService walletService) {
        super(requestMapper, responseMapper);
        this.walletService = walletService;
    }

    @Override
    protected PlayerBalanceData executeService(BalanceContext context) {
        return walletService.process(context);
    }
}
