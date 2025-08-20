package com.nextgen.gameaggregator.core.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VendorSignatureValidator {
    String getVendorClassName();
    boolean shouldValidate(HttpServletRequest request, String endpoint);
    void validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException;
    VendorErrorResponse onInvalidSignature(HttpServletRequest request);
}
