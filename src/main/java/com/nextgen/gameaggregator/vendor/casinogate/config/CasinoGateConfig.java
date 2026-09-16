package com.nextgen.gameaggregator.vendor.casinogate.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Endpoints;
import org.springframework.stereotype.Component;

@Component
public class CasinoGateConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = Endpoints.CLASS_NAME;
    public static final Integer ID = 105;

    public CasinoGateConfig() {
        super(Endpoints.CLASS_NAME);
    }

    @Override
    protected void overrideDefaults() {
        setWalletServiceLegacyEnabled(false);
    }
}
