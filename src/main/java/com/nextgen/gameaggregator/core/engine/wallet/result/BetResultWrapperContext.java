package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;

@Data
public class BetResultWrapperContext {
    private BetResultContext betResultContext;
    private BaseVendorService vendorService;
    private BetResultConfig config = new BetResultConfig();
}
