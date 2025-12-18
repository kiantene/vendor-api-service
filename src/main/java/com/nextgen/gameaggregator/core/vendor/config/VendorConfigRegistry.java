package com.nextgen.gameaggregator.core.vendor.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VendorConfigRegistry {
    private static final String CALLBACK_PREFIX_V1 = "/api/v1/";
    private final Map<String, VendorConfig> map;

    public VendorConfigRegistry(List<VendorConfig> handlers) {
        this.map = handlers.stream()
                .filter(h -> h.getVendorClassName() != null)
                .collect(Collectors.toMap(
                        VendorConfig::getVendorClassName,
                        Function.identity()
                ));
    }

    public boolean exists(String vendorClassName) { return map.containsKey(vendorClassName); }

    public VendorConfig get(String vendorClassName) {
        return map.get(vendorClassName);
    }

    public Optional<VendorConfig> getByRequestURI(String requestURI) {
        for (var entry : map.entrySet()) {
            if (requestURI.startsWith(getCallback(entry.getKey()))) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private String getCallback(String className) {
        return CALLBACK_PREFIX_V1 + className;
    }
}
