package com.nextgen.gameaggregator.vendor.evoplay.config;

import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ActionName;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EvoplayConfig extends AbstractVendorConfig {

    public static final String CLASS_NAME = "evoplay";
    private final VendorCallbackRouteResolver routeResolver;

    public EvoplayConfig(MigrationRoundDataService migrationRoundDataService) {
        super(CLASS_NAME);
        this.routeResolver = new EvoplayRouteResolver(this, migrationRoundDataService);
    }

    @Override
    protected void overrideDefaults() {
        // v2 routing OFF by default; enabled per-region at runtime via the Couchbase
        // VendorConfig (callbackRoutingEnabled). Keeps cutover a config toggle, not a deploy.
        setCallbackRoutingEnabled(false);
        setWalletServiceLegacyEnabled(false);
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
