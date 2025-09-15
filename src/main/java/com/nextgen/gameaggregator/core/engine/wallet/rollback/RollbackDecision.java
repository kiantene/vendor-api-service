package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackDecisionType;

public record RollbackDecision(RollbackDecisionType type, String reason) {
    public boolean isAllowed()  { return type == RollbackDecisionType.ALLOW; }
    public boolean isRejected() { return type == RollbackDecisionType.REJECT; }
    public boolean isNoOp()     { return type == RollbackDecisionType.NO_OP; }
}
