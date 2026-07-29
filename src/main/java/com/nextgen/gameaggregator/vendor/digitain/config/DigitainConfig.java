package com.nextgen.gameaggregator.vendor.digitain.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import org.springframework.stereotype.Component;

@Component
public class DigitainConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = EndPoints.CLASS_NAME;
    public static final int DIGITAIN_VENDOR_ID = 102;
    public static final String HEADER_AUTHORIZATION = "SecretKey";

    public DigitainConfig() {
        super(CLASS_NAME);
        overrideDefaults();
    }

    @Override
    protected void overrideDefaults() {
        setTransactionHistoryEnabled(true);
    }

}