package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import lombok.Data;

@Data
public class AdjustmentConfig {
    private boolean returnSuccessOnDuplicate = false;
    private boolean calculateAdjustmentAmount = true;

    public AdjustmentConfig returnSuccessOnDuplicate(boolean flag) {
        this.returnSuccessOnDuplicate = flag;
        return this;
    }

    public AdjustmentConfig calculateAdjustmentAmount(boolean flag) {
        this.calculateAdjustmentAmount = flag;
        return this;
    }
}
