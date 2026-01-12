package com.nextgen.gameaggregator.core.vendor.config;

import com.nextgen.gameaggregator.core.registry.VendorConfigExtension;
import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Default Values to be managed at AbstractVendorConfig
 */
public interface VendorIntegrationConfig extends VendorConfigExtension {
    int getTimeoutInMillis();

    boolean isNewFramework();

    boolean isTransactionHistoryEnabled();

    boolean isWalletServiceLegacyEnabled();

    boolean isCallbackRoutingEnabled();

   Optional<VendorCallbackRouteResolver> callbackRouteResolver();

   boolean isMigrationVendor();

}
