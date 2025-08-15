package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import lombok.Data;

@Data
public class BetRollbackWrapperContext {
    private BetRollbackContext betRollbackContext;
    private boolean retrieveSettledBet = false;

    public BetRollbackWrapperContext(BetRollbackContext context) {
        this.betRollbackContext = context;
    }
}
