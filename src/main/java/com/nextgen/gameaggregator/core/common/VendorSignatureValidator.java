package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.exception.SignatureValidationException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VendorSignatureValidator {
    String getVendorClassName();
    void validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException;

    default VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(
                Map.of("error", "Invalid signature")
        );
    }
}
