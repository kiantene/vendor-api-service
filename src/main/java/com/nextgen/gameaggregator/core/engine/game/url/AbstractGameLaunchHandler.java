package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.gameaggregator.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;
import java.util.Optional;

public abstract class AbstractGameLaunchHandler<R, T> implements GameLaunchHandler<R, T> {
    /**
     * Default response type is String.
     * Subclasses must override for typed JSON parsing.
     */
    @Override
    public ParameterizedTypeReference<T> getResponseType() {
        throw new UnsupportedOperationException("Subclasses must override getResponseType()");
    }

    // --------------------------------------------
    // Utility Methods for Subclass Use
    // --------------------------------------------

    /**
     * Utility: Get a required credential from the vendor credentials map.
     * @throws com.nextgen.gameaggregator.core.exception.InternalConfigurationException if key is missing or empty
     */
    protected VendorLineCredential getRequiredCredential(Map<String, VendorLineCredential> credentials, String key) {
        return Optional.ofNullable(credentials.get(key))
                .filter(c -> c.getValue() != null && !c.getValue().isBlank())
                .orElseThrow(() -> new InternalConfigurationException(key + " is missing or has no value."));
    }

    /**
     * Returns just the credential value string.
     * Throws if missing or blank.
     */
    protected String getRequiredCredentialValue(Map<String, VendorLineCredential> credentials, String key) {
        return getRequiredCredential(credentials, key).getValue();
    }
}
