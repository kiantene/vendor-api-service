package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import lombok.Data;

@Data
public class BetRollbackWrapperContext {
    private BetRollbackContext betRollbackContext;
    private BetRollbackConfig config;

    public BetRollbackWrapperContext(BetRollbackContext context) {
        this.betRollbackContext = context;
        this.config = new BetRollbackConfig();
    }
}
