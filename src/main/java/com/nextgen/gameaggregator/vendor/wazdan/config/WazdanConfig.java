package com.nextgen.gameaggregator.vendor.wazdan.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.wazdan.constant.EndPoints;
import org.springframework.stereotype.Component;

@Component
public class WazdanConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = EndPoints.CLASS_NAME;
    public static final Integer ID = 116;

    public WazdanConfig() {
        super(CLASS_NAME);
        overrideDefaults();
    }

    @Override
    protected void overrideDefaults() {
        setTransactionHistoryEnabled(true);
        setWalletServiceLegacyEnabled(false);
    }

}

