package com.nextgen.gameaggregator.core.vendor.config;

import com.nextgen.gameaggregator.vendor.Vendors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Service
@Slf4j
public class VendorConfigService {

    private final VendorConfigRegistry registry;
    private final HashMap<String, Boolean> vendorEnumMap; // backward compatibility

    public VendorConfigService(VendorConfigRegistry registry) {
        this.registry = registry;
        this.vendorEnumMap = new HashMap<>();

        for (Vendors v : Vendors.values()) {
            if (!this.vendorEnumMap.containsKey(v.getClassName())) {
                this.vendorEnumMap.put(v.getClassName(), v.isNewFramework());
            }
        }
    }

    public boolean exists(String className) {
        return registry.exists(className);
    }

    public VendorConfig getConfig(String className) {
        return registry.get(className);
    }

    public Optional<VendorConfig> getConfigByRequestURI(String requestURI) {
        Optional<VendorConfig> vendorConfig = registry.getByRequestURI(requestURI);

        if (vendorConfig.isPresent()) {
            return vendorConfig;
        }

        // for backward compatibility
        Vendors vendor = Vendors.fromRequestURI(requestURI);

        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.of(new VendorConfig() {
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
            public String getVendorClassName() {
                return vendor.getClassName();
            }
        });
    }

    public int getTimeoutInMillis(String className) {
        Optional<VendorConfig> configOpt = Optional.of(getConfig(className));

        return configOpt.map(VendorConfig::getTimeoutInMillis).orElse(AbstractVendorConfig.DEFAULT_TIMEOUT);
    }

    public boolean isNewFramework(String className) {
        Optional<VendorConfig> configOpt = Optional.of(getConfig(className));

        // if vendor config handler exists, get value from handler
        // otherwise get from Vendors enum as backward-compatibility
        return configOpt.map(VendorConfig::isNewFramework).orElseGet(() -> this.vendorEnumMap.getOrDefault(className, false));
    }

    public boolean isWalletServiceLegacyEnabled(String className) {
        VendorConfig config = getConfig(className);

        return config != null && config.isWalletServiceLegacyEnabled();
    }
}
