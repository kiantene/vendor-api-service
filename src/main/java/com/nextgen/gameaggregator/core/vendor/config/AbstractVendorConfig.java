package com.nextgen.gameaggregator.core.vendor.config;

import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.core.entity.VendorConfig;
import lombok.Data;

import java.util.function.Consumer;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
public abstract class AbstractVendorConfig implements VendorIntegrationConfig {

    public static final int DEFAULT_TIMEOUT = 4000;

    private String vendorClassName;

    /**
     * Configurabled via DB
     */
    private int timeoutInMillis = DEFAULT_TIMEOUT;
    private boolean transactionHistoryEnabled = false;
    private boolean walletServiceLegacyEnabled = true;
    private boolean callbackRoutingEnabled = false;

    /**
     * For Backward Compatability
     * TO BE REMOVED when no longer needed
     */
    @Deprecated
    protected AbstractVendorConfig() {
    }

    protected AbstractVendorConfig(String vendorClassName) {
        this.vendorClassName = vendorClassName;
        overrideDefaults();
    }

    /**
     * Used for Vendors to override the default values for:
     * 1. timeoutInMillis
     * 2. transactionHistoryEnabled
     * 3. walletServiceLegacyEnabled
     * 4. callbackRoutingEnabled
     */
    protected void overrideDefaults() {
    }

    @Override
    public boolean isNewFramework() {
        return true;
    }

    @Override
    public void updateFromDB(VendorConfig fromDB) {
        if (fromDB == null || fromDB.getIntegrationConfig() == null) {
            return;
        }

        VendorConfig.IntegrationConfig config = fromDB.getIntegrationConfig();

        applyIfPresent(config.getTimeoutMillis(), v -> timeoutInMillis = v);
        applyIfPresent(config.getTransactionHistoryEnabled(), v -> transactionHistoryEnabled = v);
        applyIfPresent(config.getWalletServiceLegacyEnabled(), v -> walletServiceLegacyEnabled = v);
        applyIfPresent(config.getCallbackRoutingEnabled(), v -> callbackRoutingEnabled = v);
    }

    private <T> void applyIfPresent(T value, Consumer<T> consumer) {
        if (value != null) consumer.accept(value);
    }

    public Optional<VendorCallbackRouteResolver> callbackRouteResolver() {
        return Optional.empty();
    }

    @Override
    public boolean isMigrationVendor() {
        return false;
    }

    @Override
    public boolean isGameCodeValidationEnabled() {
        return false;
    }
}
