package com.nextgen.gameaggregator.vendor.aviatorstudio.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.stereotype.Component;

@Component
public class AviatorStudioConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "aviatorstudio";

    @Override
    public String getVendorClassName() {
        return CLASS_NAME;
    }

    @Override
    public int getTimeoutInMillis() {
        return 2000;
    }
}
