package com.nextgen.gameaggregator.core.engine.wallet.adjustment;

import lombok.Data;

@Data
public class AdjustmentWrapperContext {
    private AdjustmentContext adjustmentContext;
    private AdjustmentConfig config;

    public AdjustmentWrapperContext(AdjustmentContext context) {
        this.adjustmentContext = context;
        this.config = new AdjustmentConfig();
    }
}
