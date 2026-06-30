package com.nextgen.gameaggregator.core.security.signature;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.vendor.VendorComponent;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VendorSignatureValidator extends VendorComponent {
    boolean shouldValidate(HttpServletRequest request, String endpoint);
    ValidationResult validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException;
    
    @Deprecated
    VendorErrorResponse onInvalidSignature(HttpServletRequest request);

    @Deprecated
    default VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        throw new IllegalArgumentException("onInvalidSignature with SignatureValidationException is not implemented");
    }

    @Deprecated
    default VendorErrorResponse onPlayerNotFound(SignatureValidationException exception) {
        return onInvalidSignature(exception);
    }

    // Added Overload Method to pass in the form fields. Default behaviour is to previous without form fields
    default VendorErrorResponse onInvalidSignature(SignatureValidationException exception, Map<String, String> formFields) {
        return onInvalidSignature(exception);
    }

    // Added Overload Method to pass in the form fields. Default behaviour is to previous without form fields
    default VendorErrorResponse onPlayerNotFound(SignatureValidationException exception, Map<String, String> formFields) {
        return onPlayerNotFound(exception);
    }

    default boolean useNewEvents() {
        return false;
    }
}
