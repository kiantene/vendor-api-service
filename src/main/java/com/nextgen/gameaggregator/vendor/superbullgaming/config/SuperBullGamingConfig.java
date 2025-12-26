package com.nextgen.gameaggregator.vendor.superbullgaming.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import org.springframework.stereotype.Component;

@Component
public class SuperBullGamingConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "superbullgaming";

    @Override
    public String getVendorClassName() {
        return CLASS_NAME;
    }
}
