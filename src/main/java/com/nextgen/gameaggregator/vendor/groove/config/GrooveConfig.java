package com.nextgen.gameaggregator.vendor.groove.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import org.springframework.stereotype.Component;

@Component
public class GrooveConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = EndPoints.CLASS_NAME;

    public GrooveConfig() {
        super(CLASS_NAME);
        overrideDefaults();
    }

    @Override
    protected void overrideDefaults() {
        setWalletServiceLegacyEnabled(false);
        setTransactionHistoryEnabled(true);
    }

}
