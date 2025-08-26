package com.nextgen.gameaggregator.core.security;

import com.nextgen.gameaggregator.core.security.decrypter.VendorDecrypter;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureValidator;
import com.nextgen.gameaggregator.vendor.Vendors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class VendorSecurityRegistry {
    private final Map<String, VendorSecurityAdapter> byClassName;

    public VendorSecurityRegistry(
            Collection<VendorDecrypter> decrypters,
            Collection<VendorSignatureValidator> validators
    ) {
        Map<String, DefaultVendorSecurityAdapter> tmp = new HashMap<>();

        mergeDecrypters(tmp, decrypters);
        mergeValidators(tmp, validators);

        this.byClassName = Collections.unmodifiableMap(new HashMap<>(tmp));
        if (log.isDebugEnabled()) {
            log.debug("VendorSecurityRegistry initialized: {}", byClassName.keySet());
        }
    }

    public VendorSecurityAdapter get(String className) {
        return byClassName.getOrDefault(normalize(className), DefaultVendorSecurityAdapter.empty());
    }

    public VendorSecurityAdapter get(Vendors vendor) {
        if (vendor == null) return DefaultVendorSecurityAdapter.empty();
        return get(vendor.getClassName());
    }

    public Set<String> registeredVendors() {
        return byClassName.keySet();
    }

    // --- private helpers ---

    private void mergeDecrypters(Map<String, DefaultVendorSecurityAdapter> tmp,
                                 Collection<VendorDecrypter> decrypters) {
        for (VendorDecrypter dec : decrypters) {
            String key = normalize(dec.getVendorClassName());
            DefaultVendorSecurityAdapter existing = tmp.get(key);
            if (existing != null && existing.decrypter().isPresent()) {
                throw new IllegalStateException("Duplicate VendorDecrypter for vendor '" + key + "'");
            }
            DefaultVendorSecurityAdapter merged = (existing == null)
                    ? new DefaultVendorSecurityAdapter(dec, null)
                    : new DefaultVendorSecurityAdapter(dec, existing.validator().orElse(null));
            tmp.put(key, merged);
        }
    }

    private void mergeValidators(Map<String, DefaultVendorSecurityAdapter> tmp,
                                 Collection<VendorSignatureValidator> validators) {
        for (VendorSignatureValidator val : validators) {
            String key = normalize(val.getVendorClassName());
            DefaultVendorSecurityAdapter existing = tmp.get(key);
            if (existing != null && existing.validator().isPresent()) {
                throw new IllegalStateException("Duplicate VendorSignatureValidator for vendor '" + key + "'");
            }
            DefaultVendorSecurityAdapter merged = (existing == null)
                    ? new DefaultVendorSecurityAdapter(null, val)
                    : new DefaultVendorSecurityAdapter(existing.decrypter().orElse(null), val);
            tmp.put(key, merged);
        }
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim();
    }
}
