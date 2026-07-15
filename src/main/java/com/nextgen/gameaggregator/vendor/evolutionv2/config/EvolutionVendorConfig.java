package com.nextgen.gameaggregator.vendor.evolutionv2.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import org.springframework.stereotype.Component;

/**
 * Evolution v2 promo-payout integration.
 */
@Component
public class EvolutionVendorConfig extends AbstractVendorConfig {

    public EvolutionVendorConfig() {
        super(EndPoints.CLASS_NAME);
    }
}
