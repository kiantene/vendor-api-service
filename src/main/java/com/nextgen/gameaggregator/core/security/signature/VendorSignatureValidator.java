package com.nextgen.gameaggregator.core.security.signature;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.vendor.VendorComponent;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VendorSignatureValidator extends VendorComponent {
    boolean shouldValidate(HttpServletRequest request, String endpoint);
    ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException;
    VendorErrorResponse onInvalidSignature(HttpServletRequest request);
}
