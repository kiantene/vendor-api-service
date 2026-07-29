package com.nextgen.gameaggregator.vendor.mtlive.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.stereotype.Component;

@Component
public class MtliveConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "mtlive";
    public static final Integer ID = 125;

    public MtliveConfig() {
        super(CLASS_NAME);
    }

    @Override
    public String getVendorClassName() {
        return CLASS_NAME;
    }

}