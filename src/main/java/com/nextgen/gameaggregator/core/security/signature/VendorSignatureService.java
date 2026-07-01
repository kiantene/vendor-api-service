package com.nextgen.gameaggregator.core.security.signature;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.util.ResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorSignatureService {
    private final LogContextService logContextService;

    public ValidationResult doValidation(VendorSignatureValidator validator,
                                                   ResettableRequestWrapper request,
                                                   HttpServletResponse response,
                                                   Map<String, String> parsedFields) throws IOException {

        // Check if this endpoint should be validated
        if (!validator.shouldValidate(request, request.getRequestURI())) {
            return ValidationResult.skipped(); // Skip validation, continue with request
        }

        try {
            ValidationResult result = validator.validate(request, parsedFields, request.getCachedBody());
            Map<String,String> additionalFields = result.additionalFields();
            if (!additionalFields.isEmpty()) {
                request.enrichRequestFields(additionalFields);
                additionalFields.forEach(logContextService::log);
            }

            return result;
        } catch (SignatureValidationException ex) {
            handleException(validator, request, response, ex, parsedFields);
            return ValidationResult.failure();
        }
    }

    private void handleException(VendorSignatureValidator validator,
                                 ResettableRequestWrapper request,
                                 HttpServletResponse response,
                                 SignatureValidationException ex,
                                 Map<String, String> parsedFields) throws IOException {

        LogContextHolder.get().setException(ex);
        VendorErrorResponse errorResponse = null;

        if (ex.getCause() instanceof PlayerNotFoundException) {
            errorResponse = validator.onPlayerNotFound(ex, parsedFields);
        } else {
            if (validator.useNewEvents()) {
                errorResponse = validator.onInvalidSignature(ex, parsedFields);
            } else {
                errorResponse = validator.onInvalidSignature(request);
            }
        }

        if (errorResponse == null || errorResponse.getBody() == null) {
            errorResponse = ResponseUtil.createDefaultErrorResponse("no response from validator");
        }

        ResponseUtil.writeErrorResponse(response, errorResponse.getBody(), errorResponse.getStatusCode().value(),errorResponse.getHeaders());
    }
}
