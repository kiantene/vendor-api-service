package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;

@Data
public class BetResultWrapperContext {
    private BetResultContext betResultContext;
    private BaseVendorService vendorService;
    private BetResultConfig config;

    public BetResultWrapperContext(BetResultContext context) {
        this.betResultContext = context;
        this.config = new BetResultConfig();
    }

    public static BetResultWrapperContext empty() {
        return new BetResultWrapperContext(BetResultContext.builder().build());
    }
}
