package com.nextgen.gameaggregator.core.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VendorSignatureValidator {
    String getVendorClassName();
    boolean validate(HttpServletRequest request, Map<String, String> formFields, String rawBody);

    default VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(
                Map.of("error", "Invalid signature")
        );
    }
}
