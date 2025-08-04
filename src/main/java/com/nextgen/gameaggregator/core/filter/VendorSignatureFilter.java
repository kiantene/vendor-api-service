package com.nextgen.gameaggregator.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidatorRegistry;
import com.nextgen.gameaggregator.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VendorSignatureFilter extends OncePerRequestFilter {

    private final VendorSignatureValidatorRegistry registry;
    private final RequestParserService parserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String vendorClassName = SupportedVendors.extractVendorClassName(request.getRequestURI());

        if (vendorClassName.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrapped = (ContentCachingRequestWrapper) request;
        String vendorCode = this.extractVendorCode(request.getServletPath());
        VendorSignatureValidator validator = registry.getValidator(vendorCode);

        if (validator == null) {
            filterChain.doFilter(wrapped, response);
            return;
        }

        String rawBody = new String(wrapped.getContentAsByteArray(), request.getCharacterEncoding());
        Map<String, String> parsedFields = parserService.parse(request.getContentType(), rawBody);

        try {
            validator.validate(wrapped, parsedFields, rawBody);
        } catch (SignatureValidationException ex) {
            LogContext logContext = LogContextHolder.get();
            logContext.setException(ex);
            VendorErrorResponse errorResponse = validator.onInvalidSignature(request);
            writeErrorResponse(response, errorResponse.getBody(), errorResponse.getStatusCode());
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    private String extractVendorCode(String path) {
        String[] segments = path.split("/");
        return segments.length > 3 ? segments[3] : "";
    }

    private void writeErrorResponse(HttpServletResponse response, Object responseBody, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(responseBody));
    }
}
