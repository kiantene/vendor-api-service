package com.nextgen.gameaggregator.core.filter;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.util.ResponseUtil;
import com.nextgen.gameaggregator.core.validator.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.validator.VendorSignatureValidatorRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VendorSignatureFilter extends OncePerRequestFilter {
    private final VendorSignatureValidatorRegistry registry;
    private final RequestParserService parserService;
    private final LogContextService logContextService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String vendorClassName = SupportedVendors.extractVendorClassName(request.getRequestURI());

        if (vendorClassName.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        VendorSignatureValidator validator = registry.getValidator(vendorClassName);

        if (validator == null) {
            filterChain.doFilter(request, response);
            return;
        }

        ResettableRequestWrapper wrapped = (ResettableRequestWrapper) request;
        if (doValidateSignature(validator, wrapped, response)) {
            filterChain.doFilter(wrapped, response);
        }
    }

    private boolean doValidateSignature(VendorSignatureValidator validator,
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
            if (!additionalFields.isEmpty()) {
                request.enrichRequestFields(additionalFields);
                additionalFields.forEach(logContextService::debug);
            }
            return true;
        } catch (SignatureValidationException ex) {
            LogContextHolder.get().setException(ex);
            VendorErrorResponse errorResponse = validator.onInvalidSignature(request);
            if (errorResponse == null || errorResponse.getBody() == null) {
                errorResponse = createDefaultDecryptionErrorResponse();
            }

            ResponseUtil.writeErrorResponse(response, errorResponse.getBody(), errorResponse.getStatusCode().value());
            return false;
        }
    }

    private VendorErrorResponse createDefaultDecryptionErrorResponse() {
        return new VendorErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Default signature validator - VendorSignatureValidator not implemented"
        );
    }
}
