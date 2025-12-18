package com.nextgen.gameaggregator.core.filter;

import com.nextgen.core.filter.ResettableRequestWrapper;
import com.nextgen.gameaggregator.core.common.RequestAttributes;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LoggingManager;
import com.nextgen.gameaggregator.core.util.ResponseUtil;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfig;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.vendor.Vendors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final LoggingManager loggingManager;
    private final VendorConfigService vendorConfigService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        LogContext logContext = loggingManager.onRequestStart(request);
        ResettableRequestWrapper wrappedRequest = new ResettableRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        var configOpt = vendorConfigService.getConfigByRequestURI(request.getRequestURI());
        if (configOpt.isPresent()) {
            VendorConfig vendorConfig = configOpt.get();
            request.setAttribute(RequestAttributes.VENDOR_CLASS_NAME, vendorConfig.getVendorClassName());
            logContext.setVendorClassName(vendorConfig.getVendorClassName());
        }

        try {
            cacheRawBody(request, wrappedRequest, logContext);
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            String responseBody = ResponseUtil.getResponseBody(wrappedResponse, response);
            loggingManager.onRequestCompleted(request, responseBody, null);
        }
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
