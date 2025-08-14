package com.nextgen.gameaggregator.core.filter;

import com.nextgen.gameaggregator.core.common.RequestAttributes;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LoggingManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final LoggingManager loggingManager;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        LogContext logContext = loggingManager.onRequestStart(request);
        String vendorClassName = SupportedVendors.extractVendorClassName(request.getRequestURI());
        ResettableRequestWrapper wrappedRequest = new ResettableRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        if (!vendorClassName.isBlank()) {
            request.setAttribute(RequestAttributes.VENDOR_CLASS_NAME, vendorClassName);
            logContext.setVendorClassName(vendorClassName);
        }

        try {
            cacheRawBody(request, wrappedRequest, logContext);
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            String responseBody = getResponseBody(wrappedResponse, response);
            loggingManager.onRequestCompleted(request, responseBody, null);
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper wrappedResponse, HttpServletResponse response)
            throws IOException {
        byte[] content = wrappedResponse.getContentAsByteArray();
        wrappedResponse.copyBodyToResponse();
        return new String(content, response.getCharacterEncoding());
    }

    private void cacheRawBody(HttpServletRequest request,
                              ResettableRequestWrapper wrappedRequest,
                              LogContext logContext) {
        String method = request.getMethod();
        boolean hasBody = "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);

        String rawBody = hasBody
                ? wrappedRequest.getCachedBody()
                : request.getQueryString();

        request.setAttribute(RequestAttributes.RAW_BODY, rawBody);
        logContext.setBody(rawBody);
    }
}
