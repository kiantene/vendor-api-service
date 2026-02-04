package com.nextgen.gameaggregator.vendor.vplus.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import org.springframework.stereotype.Component;

@Component
public class VplusConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = EndPoints.CLASS_NAME;

    public VplusConfig() {
        super(CLASS_NAME);
        overrideDefaults();
    }

    @Override
    protected void overrideDefaults() {
//        setWalletServiceLegacyEnabled(false);
        setTransactionHistoryEnabled(true);
    }

}
