package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import lombok.Data;

@Data
public class BetRollbackConfig {
    private RollbackType rollbackType;

    public BetRollbackConfig() {

    }

    public BetRollbackConfig rollbackType(RollbackType rollbackType) {
        this.rollbackType = rollbackType;
        return this;
    }
}
