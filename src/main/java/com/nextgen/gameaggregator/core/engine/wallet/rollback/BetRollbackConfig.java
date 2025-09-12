package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import lombok.Data;

@Data
public class BetRollbackConfig {
    private RollbackType rollbackType;
    private boolean allowRollbackForSettledBet;

    public BetRollbackConfig() {
        this.allowRollbackForSettledBet = false;
    }

    public BetRollbackConfig rollbackType(RollbackType rollbackType) {
        this.rollbackType = rollbackType;
        return this;
    }

    public BetRollbackConfig allowRollbackForSettledBet(boolean flag) {
        this.allowRollbackForSettledBet = flag;
        return this;
    }
}
