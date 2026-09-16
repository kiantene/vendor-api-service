package com.nextgen.gameaggregator.vendor.esoterica.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import org.springframework.stereotype.Component;

@Component
public class EsotericaConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = EndPoints.CLASS_NAME;

    public EsotericaConfig() {
        super(CLASS_NAME);
    }

    @Override
    public String getVendorClassName() {
        return CLASS_NAME;
    }

    @Override
    protected void overrideDefaults() {
        setWalletServiceLegacyEnabled(false);
    }
}
