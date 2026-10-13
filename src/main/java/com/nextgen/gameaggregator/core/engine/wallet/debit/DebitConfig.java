package com.nextgen.gameaggregator.core.engine.wallet.debit;

import lombok.Data;

/**
 * Per-request configuration for the debit pipeline. Mirrors {@link com.nextgen.gameaggregator.core.engine.wallet.bet.BetConfig}.
 * Subclasses of {@link AbstractDebitController} customize via {@code configure(DebitConfig, Q)}.
 */
@Data
public class DebitConfig {
    /** When true, a duplicate debit returns the prior balance as success instead of throwing. */
    private boolean returnSuccessOnDuplicate;

    public DebitConfig() {
        this.returnSuccessOnDuplicate = false;
    }

    public DebitConfig returnSuccessOnDuplicate(boolean flag) {
        this.returnSuccessOnDuplicate = flag;
        return this;
    }
}
