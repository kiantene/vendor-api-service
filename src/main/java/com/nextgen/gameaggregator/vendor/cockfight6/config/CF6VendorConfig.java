package com.nextgen.gameaggregator.vendor.cockfight6.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.stereotype.Component;

@Component
public class CF6VendorConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "cockfight6";

    public CF6VendorConfig() {
        super(CLASS_NAME);
    }

    @Override
    protected void overrideDefaults() {
        setWalletServiceLegacyEnabled(false);
    }
}
