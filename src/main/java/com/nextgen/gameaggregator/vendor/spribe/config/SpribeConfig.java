package com.nextgen.gameaggregator.vendor.spribe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.vendor.config.AbstractVendorConfig;
import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import com.nextgen.gameaggregator.vendor.spribe.constant.Endpoints;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class SpribeConfig extends AbstractVendorConfig {
    public static final String CLASS_NAME = "spribe";
    private final VendorCallbackRouteResolver routeResolver;

    public SpribeConfig(ObjectMapper objectMapper,
                        AgentPlayerDataService agentPlayerDataService,
                        VendorPlayerDataService vendorPlayerDataService,
                        GameTransactionService gameTransactionService) {
        super(CLASS_NAME);
        this.routeResolver = new SpribeRouteResolver(
                objectMapper,
                agentPlayerDataService,
                vendorPlayerDataService,
                gameTransactionService,
                this
        );
    }

    @Override
    protected void overrideDefaults() {
        setCallbackRoutingEnabled(true);
    }

    @Override
    public Optional<VendorCallbackRouteResolver> callbackRouteResolver() {
        return Optional.of(routeResolver);
    }

    @Override
    public boolean isMigrationVendor() {
        return true;
    }

    /**
     * EndPoints that needs to check for Bet Transactions before determining to route to v1 or v2ßßß
     * @return
     */
    public Set<String> getRoutingEndPoints() {
        return Set.of(
                "/"+Endpoints.PATH+Endpoints.DEPOSIT,
                "/"+Endpoints.PATH+Endpoints.ROLLBACK
                );
    }
}
