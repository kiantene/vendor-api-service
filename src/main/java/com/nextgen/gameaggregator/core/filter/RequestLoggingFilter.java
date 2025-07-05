package com.nextgen.gameaggregator.core.filter;

import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LoggingManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final LoggingManager loggingManager;

    public RequestLoggingFilter(LoggingManager loggingManager) {
        this.loggingManager = loggingManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String vendorClassName = SupportedVendors.shouldApplyFilter(requestURI);

        if (vendorClassName != null) {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

            try {
                if (hasBody(request)) {
                    String rawBody = getRawRequestBody(wrappedRequest, request);
                    LogContext logContext = loggingManager.onRequestStart(request, rawBody);
                    logContext.setVendorClassName(vendorClassName);
                    filterChain.doFilter(wrappedRequest, wrappedResponse);
                } else {
                    filterChain.doFilter(request, response);
                }
            } finally {
                String responseBody = getResponseBody(wrappedResponse, response);
                loggingManager.onRequestCompleted(request, responseBody, null);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String getRawRequestBody(ContentCachingRequestWrapper wrappedRequest, HttpServletRequest request) throws UnsupportedEncodingException {
        wrappedRequest.getParameterMap();

        byte[] body = wrappedRequest.getContentAsByteArray();
        String rawBody = "";
        if (body.length > 0) {

            // TODO: need to test encoding
            rawBody = new String(body, request.getCharacterEncoding());
            request.setAttribute("rawBody", rawBody); // store for later
        }
        return rawBody;
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
