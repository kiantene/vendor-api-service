package com.nextgen.gameaggregator.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidatorRegistry;
import com.nextgen.gameaggregator.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

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

    private void writeErrorResponse(HttpServletResponse response, Object responseBody, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(responseBody));
    }

    private boolean doValidateSignature(VendorSignatureValidator validator,
                                        ResettableRequestWrapper request,
                                        HttpServletResponse response) throws IOException {
        try {
            String rawBody = request.getCachedBody();
            Map<String, String> parsedFields = parserService.parse(request.getContentType(), rawBody);
            validator.validate(request, parsedFields, rawBody);
            return true;
        } catch (SignatureValidationException ex) {
            LogContextHolder.get().setException(ex);
            VendorErrorResponse errorResponse = validator.onInvalidSignature(request);
            if (errorResponse == null || errorResponse.getBody() == null) {
                errorResponse = new VendorErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Default signature validator - VendorSignatureValidator not implemented"
                );
            }

            writeErrorResponse(response, errorResponse.getBody(), errorResponse.getStatusCode().value());
            return false;
        }
    }
}
