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
        // v2 routing is the post-cutover state; the Couchbase VendorConfig can still
        // flip it per-region at runtime. Defaulting to true keeps a pod that boots
        // before the first successful config refresh from sending v2 rounds down v1.
        setCallbackRoutingEnabled(true);
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
