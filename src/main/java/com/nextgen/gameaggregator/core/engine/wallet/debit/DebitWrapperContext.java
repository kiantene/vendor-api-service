package com.nextgen.gameaggregator.core.engine.wallet.debit;

import lombok.Data;

/** Holds the per-request {@link DebitContext} + {@link DebitConfig} pair. */
@Data
public class DebitWrapperContext {
    private DebitContext debitContext;
    private DebitConfig config;

    public DebitWrapperContext(DebitContext context) {
        this.debitContext = context;
        this.config = new DebitConfig();
    }
}
