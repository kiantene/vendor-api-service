package com.nextgen.gameaggregator.core.engine.wallet.bet;

import lombok.Data;

@Data
public class BetConfig {
    private boolean returnSuccessOnDuplicate;
    private boolean allowMultipleBet;

    public BetConfig() {
        this.returnSuccessOnDuplicate = false;
        this.allowMultipleBet = false;
    }

    public BetConfig returnSuccessOnDuplicate(boolean flag) {
        this.returnSuccessOnDuplicate = flag;
        return this;
    }

    public BetConfig allowMultipleBet(boolean flag) {
        this.allowMultipleBet = flag;
        return this;
    }
}
