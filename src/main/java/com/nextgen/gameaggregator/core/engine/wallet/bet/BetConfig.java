package com.nextgen.gameaggregator.core.engine.wallet.bet;

import lombok.Data;

@Data
public class BetConfig {
    private boolean returnSuccessOnDuplicate;

    public BetConfig() {
        this.returnSuccessOnDuplicate = false;
    }

    public BetConfig returnSuccessOnDuplicate(boolean flag) {
        this.returnSuccessOnDuplicate = flag;
        return this;
    }
}
