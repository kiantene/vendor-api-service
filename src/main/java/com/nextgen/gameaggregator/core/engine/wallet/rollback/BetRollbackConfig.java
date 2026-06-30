package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import lombok.Data;

@Data
public class BetRollbackConfig {
    private RollbackType rollbackType;
    private boolean allowRollbackForSettledBet;
    private boolean returnSuccessOnDuplicate;
    private boolean validateSessionToken;
    private boolean allowRollbackWhenRoundHasResult;

    public BetRollbackConfig() {
        this.allowRollbackForSettledBet = false;
        this.returnSuccessOnDuplicate = false;
        this.validateSessionToken = false;
        this.allowRollbackWhenRoundHasResult = true;
    }

    @Deprecated(forRemoval = true)
    public BetRollbackConfig rollbackType(com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType oldType) {
        this.rollbackType = com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType.valueOf(oldType.name());
        return this;
    }

    public BetRollbackConfig rollbackType(RollbackType rollbackType) {
        this.rollbackType = rollbackType;
        return this;
    }

    public BetRollbackConfig allowRollbackForSettledBet(boolean flag) {
        this.allowRollbackForSettledBet = flag;
        return this;
    }

    public BetRollbackConfig returnSuccessOnDuplicate(boolean flag) {
        this.returnSuccessOnDuplicate = flag;
        return this;
    }

    public BetRollbackConfig validateSessionToken(boolean flag) {
        this.validateSessionToken = flag;
        return this;
    }

    public BetRollbackConfig allowRollbackWhenRoundHasResult(boolean flag) {
        this.allowRollbackWhenRoundHasResult = flag;
        return this;
    }

    public boolean isRollbackByRound() {
        return rollbackType == RollbackType.BY_ROUND;
    }

    public boolean isRollbackByBet() {
        return rollbackType == RollbackType.BY_BET;
    }

    @Deprecated(forRemoval = true)
    public void setRollbackType(com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType oldType) {
        this.rollbackType = com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType.valueOf(oldType.name());
    }
}
