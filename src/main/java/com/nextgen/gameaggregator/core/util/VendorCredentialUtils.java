package com.nextgen.gameaggregator.core.util;

import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VendorCredentialUtils {

    public VendorCredentialAccessor of(Map<String, VendorLineCredential> credentials) {
        return new VendorCredentialAccessor(credentials);
    }
}
