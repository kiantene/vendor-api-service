package com.nextgen.gameaggregator.core.engine.promo.payout;

import lombok.Data;

@Data
public class PromoPayoutConfig {
    private boolean batch;

    public PromoPayoutConfig() {
        this.batch = false;
    }

    public PromoPayoutConfig batch(boolean flag) {
        this.batch = flag;
        return this;
    }
}
