package com.nextgen.gameaggregator.vendor.egtdigital.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import org.springframework.stereotype.Component;


@Component
public class EgtConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = EndPoints.CLASS_NAME;

    public EgtConfig() {
        super(CLASS_NAME);
        overrideDefaults();
    }

    @Override
    protected void overrideDefaults() {
        setWalletServiceLegacyEnabled(false);
        setTransactionHistoryEnabled(true);
    }
}
