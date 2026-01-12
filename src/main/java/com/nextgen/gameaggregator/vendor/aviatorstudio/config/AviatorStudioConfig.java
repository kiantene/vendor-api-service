package com.nextgen.gameaggregator.vendor.aviatorstudio.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.stereotype.Component;

@Component
public class AviatorStudioConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "aviatorstudio";

    public AviatorStudioConfig() {
        super(CLASS_NAME);
    }

    @Override
    protected void overrideDefaults() {
        setTimeoutInMillis(2000);
    }
}
