package com.nextgen.gameaggregator.vendor.koolbet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class KoolbetConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "koolbet";
    private final VendorCallbackRouteResolver routeResolver;

    public KoolbetConfig(MigrationRoundDataService migrationRoundDataService, ObjectMapper objectMapper) {
        super(CLASS_NAME);
        this.routeResolver = new KoolbetRouteResolver(this, migrationRoundDataService, objectMapper);
    }

    @Override
    protected void overrideDefaults() {
        // v2 routing OFF by default; enabled per-region at runtime via the Couchbase
        // VendorConfig (callbackRoutingEnabled). Keeps cutover a config toggle, not a deploy.
        setCallbackRoutingEnabled(false);
        setWalletServiceLegacyEnabled(false);
    }

    @Override
    public boolean isGameCodeValidationEnabled() {
        return true;
    }

    @Override
    public Optional<VendorCallbackRouteResolver> callbackRouteResolver() {
        return Optional.of(routeResolver);
    }

    @Override
    public boolean isMigrationVendor() {
        return true;
    }
}
