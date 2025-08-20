package com.nextgen.gameaggregator.core.util;

import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;

import java.util.Map;
import java.util.Optional;

public class VendorCredentialAccessor {

    private final Map<String, VendorLineCredential> credentials;

    public VendorCredentialAccessor(Map<String, VendorLineCredential> credentials) {
        this.credentials = credentials;
    }

    public VendorLineCredential get(String key) {
        return Optional.ofNullable(credentials.get(key))
                .filter(c -> c.getValue() != null && !c.getValue().isBlank())
                .orElseThrow(() -> new InternalConfigurationException(key + " is missing or has no value."));
    }

    public String getValue(String key) {
        return get(key).getValue();
    }

    public Optional<String> getOptionalValue(String key) {
        return Optional.ofNullable(credentials.get(key))
                .map(VendorLineCredential::getValue)
                .filter(v -> !v.isBlank());
    }

    public String getOrDefault(String key, String defaultValue) {
        return getOptionalValue(key).orElse(defaultValue);
    }
}
