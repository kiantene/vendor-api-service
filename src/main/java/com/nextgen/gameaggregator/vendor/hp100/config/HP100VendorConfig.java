package com.nextgen.gameaggregator.vendor.hp100.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.stereotype.Component;

@Component
public class HP100VendorConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "hp100";

    public HP100VendorConfig() {
        super(CLASS_NAME);
    }

    @Override
    protected void overrideDefaults() {
        setWalletServiceLegacyEnabled(false);
    }

    
}
