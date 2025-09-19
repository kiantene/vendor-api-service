package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.enums.BetResultDecisionType;

public record BetResultDecision (BetResultDecisionType type, String reason) {
    public boolean isAllowed()  { return type == BetResultDecisionType.ALLOW; }
    public boolean isRejected() { return type == BetResultDecisionType.REJECT; }
    public boolean isNoOp()     { return type == BetResultDecisionType.NO_OP; }
}
