package com.nextgen.gameaggregator.core.engine.promo.payout;

import lombok.Data;

@Data
public class PromoPayoutWrapperContext {
    private PromoPayoutContext context;
    private PromoPayoutConfig config;

    public PromoPayoutWrapperContext(PromoPayoutContext context) {
        this.context = context;
        this.config = new PromoPayoutConfig();
    }
}
