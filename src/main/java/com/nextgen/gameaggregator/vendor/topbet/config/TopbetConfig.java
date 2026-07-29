package com.nextgen.gameaggregator.vendor.topbet.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.topbet.constant.EndPoints;
import org.springframework.stereotype.Component;

@Component
public class TopbetConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = EndPoints.CLASS_NAME;
    public static final Integer ID = 105;

    public TopbetConfig() {
        super(CLASS_NAME);
        overrideDefaults();
    }

    @Override
    protected void overrideDefaults() {
        setTransactionHistoryEnabled(true);
        setWalletServiceLegacyEnabled(false);
    }

}

