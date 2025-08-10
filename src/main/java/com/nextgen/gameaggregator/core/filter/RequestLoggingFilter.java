package com.nextgen.gameaggregator.core.filter;

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
        String requestURI = request.getRequestURI();
        String vendorClassName = SupportedVendors.extractVendorClassName(requestURI);

        if (!vendorClassName.isEmpty()) {
            request.setAttribute("vendorClassName", vendorClassName);
            logContext.setVendorClassName(vendorClassName);
            ResettableRequestWrapper wrappedRequest = new ResettableRequestWrapper(request);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

            try {
                String rawBody;
                if (hasBody(request)) { // POST,PUT,PATCH
                    rawBody = wrappedRequest.getCachedBody();
                } else { // GET
                    rawBody = request.getQueryString();
                }
                logContext.setBody(rawBody);
                request.setAttribute("rawBody", rawBody);
                filterChain.doFilter(wrappedRequest, wrappedResponse);
            } finally {
                String responseBody = getResponseBody(wrappedResponse, response);
                loggingManager.onRequestCompleted(request, responseBody, null);
            }
        } else {
            filterChain.doFilter(request, response);
            loggingManager.onRequestCompleted(request, "", null);
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper wrappedResponse, HttpServletResponse response) throws IOException {
        byte[] content = wrappedResponse.getContentAsByteArray();
        wrappedResponse.copyBodyToResponse();
        return new String(content, response.getCharacterEncoding());
    }

    private boolean hasBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}
