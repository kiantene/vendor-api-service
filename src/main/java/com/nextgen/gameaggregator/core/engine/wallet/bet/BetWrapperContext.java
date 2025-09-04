package com.nextgen.gameaggregator.core.engine.wallet.bet;

import lombok.Data;

@Data
public class BetWrapperContext {
    private BetContext betContext;
    private BetConfig config;

    public BetWrapperContext(BetContext context) {
        this.betContext = context;
        this.config = new BetConfig();
    }
}
