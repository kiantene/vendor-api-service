package com.nextgen.gameaggregator.vendor.endorphina.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.stereotype.Component;

@Component
public class EndorphinaConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "endorphina";

    public EndorphinaConfig() {
        super(CLASS_NAME);
    }
}

