package com.nextgen.gameaggregator.core.vendor.config;

import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.core.entity.VendorConfig;
import com.nextgen.gameaggregator.core.registry.VendorConfigExtension;
import com.nextgen.gameaggregator.core.registry.VendorConfigRegistry;
import com.nextgen.gameaggregator.core.service.AbstractVendorConfigService;
import com.nextgen.gameaggregator.vendor.Vendors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class VendorConfigService extends AbstractVendorConfigService {
    private static final String CALLBACK_PREFIX_V1 = "/api/v1/";

    private final HashMap<String, Boolean> vendorEnumMap; // backward compatibility

    public VendorConfigService(VendorConfigRegistry registry) {
        super(registry);
        this.vendorEnumMap = new HashMap<>();

        for (Vendors v : Vendors.values()) {
            if (!this.vendorEnumMap.containsKey(v.getClassName())) {
                this.vendorEnumMap.put(v.getClassName(), v.isNewFramework());
            }
        }
    }

    public Optional<VendorIntegrationConfig> getConfigByRequestURI(String requestURI) {
        Optional<VendorConfigExtension> vendorConfig = getRegistry().getByRequestURI(requestURI, CALLBACK_PREFIX_V1);

        if (vendorConfig.isPresent()) {
            return vendorConfig.map(VendorIntegrationConfig.class::cast);
        }

        // for backward compatibility
        Vendors vendor = Vendors.fromRequestURI(requestURI);

        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.of(new VendorIntegrationConfig() {
            @Override
            public int getTimeoutInMillis() {
                return vendor.getTimeoutMillis();
            }

            @Override
            public boolean isNewFramework() {
                return vendor.isNewFramework();
            }

            @Override
            public boolean isTransactionHistoryEnabled() {
                return false;
            }

            @Override
            public boolean isWalletServiceLegacyEnabled() {
                return true;
            }

            @Override
            public boolean isCallbackRoutingEnabled() {
                return false;
            }

            @Override
            public boolean isGameCodeValidationEnabled() {
                return false;
            }

            @Override
            public Optional<VendorCallbackRouteResolver> callbackRouteResolver() {
                return Optional.empty();
            }

            @Override
            public boolean isMigrationVendor() {
                return false;
            }

            @Override
            public String getVendorClassName() {
                return vendor.getClassName();
            }
        });
    }

    public int getTimeoutInMillis(String className) {
        return getVendorIntegrationConfig(className)
                .map(VendorIntegrationConfig::getTimeoutInMillis)
                .orElse(AbstractVendorConfig.DEFAULT_TIMEOUT);
    }

    public boolean isNewFramework(String className) {
        // if vendor config handler exists, get value from handler
        // otherwise get from Vendors enum as backward-compatibility
        return getVendorIntegrationConfig(className)
                .map(VendorIntegrationConfig::isNewFramework)
                .orElseGet(() -> this.vendorEnumMap.getOrDefault(className, false));
    }

    public boolean isTransactionHistoryEnabled(String className) {
        Optional<VendorIntegrationConfig> config = getVendorIntegrationConfig(className);

        return config.isPresent() && config.get().isTransactionHistoryEnabled();
    }

    public boolean isWalletServiceLegacyEnabled(String className) {
        Optional<VendorIntegrationConfig> config = getVendorIntegrationConfig(className);

        return !config.isPresent() || config.get().isWalletServiceLegacyEnabled();
    }

    public Optional<VendorIntegrationConfig> getVendorIntegrationConfig(String className) {
        VendorConfigExtension config = getConfig(className);
        if (config == null) {
            return Optional.empty();
        } else {
            return Optional.of(config).map(VendorIntegrationConfig.class::cast);
        }
    }

    public void updateVendorConfigInRegistry(Map<String, VendorConfig> map) {
        map.forEach((key, value) -> {
            if (exists(key)) {
                getConfig(key).updateFromDB(value);
            }
        });
    }

    public boolean isCallbackRoutingEnabled(String className) {
        Optional<VendorIntegrationConfig> config = getVendorIntegrationConfig(className);

        return config.isPresent() && config.get().isCallbackRoutingEnabled();
    }

    public boolean isMigrationVendor(String className) {
        Optional<VendorIntegrationConfig> config = getVendorIntegrationConfig(className);

        return config.isPresent() && config.get().isMigrationVendor();
    }

    public boolean isGameCodeValidationEnabled(String className) {
        Optional<VendorIntegrationConfig> config = getVendorIntegrationConfig(className);

        return config.isPresent() && config.get().isGameCodeValidationEnabled();
    }
}
