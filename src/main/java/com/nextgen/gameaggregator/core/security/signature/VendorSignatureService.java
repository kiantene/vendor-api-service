package com.nextgen.gameaggregator.core.security.signature;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
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
    private final RequestParserService parserService;
    private final LogContextService logContextService;

    public boolean doValidation(VendorSignatureValidator validator,
                                ResettableRequestWrapper request,
                                HttpServletResponse response) throws IOException {

        // Check if this endpoint should be validated
        if (!validator.shouldValidate(request, request.getRequestURI())) {
            return true; // Skip validation, continue with request
        }

        try {
            String rawBody = request.getCachedBody();
            Map<String, String> parsedFields = parserService.parse(request.getContentType(), rawBody);
            Map<String, String> additionalFields = validator.validate(request, parsedFields, rawBody);
            if (additionalFields != null && !additionalFields.isEmpty()) {
                request.enrichRequestFields(additionalFields);
                additionalFields.forEach(logContextService::debug);
            }
            return true;
        } catch (SignatureValidationException ex) {
            LogContextHolder.get().setException(ex);
            VendorErrorResponse errorResponse = validator.onInvalidSignature(request);
            if (errorResponse == null || errorResponse.getBody() == null) {
                errorResponse = ResponseUtil.createDefaultErrorResponse("no response from validator");
            }

            ResponseUtil.writeErrorResponse(response, errorResponse.getBody(), errorResponse.getStatusCode().value());
            return false;
        }
    }
}
