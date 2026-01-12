package com.nextgen.gameaggregator.core.vendor.routing;

import com.nextgen.gameaggregator.core.common.RequestAttributes;
import com.nextgen.gameaggregator.core.vendor.config.VendorIntegrationConfig;
import jakarta.servlet.http.HttpServletRequest;

public record VendorRouteContext(
        VendorIntegrationConfig vendorConfig,
        String originalUri,
        String httpMethod,
        String contentType,
        String rawBody // from RequestAttributes.RAW_BODY
) {

    public static VendorRouteContext of(VendorIntegrationConfig vendorConfig, HttpServletRequest request) {
        String rawBody = (String) request.getAttribute(RequestAttributes.RAW_BODY);

        return new VendorRouteContext(
                vendorConfig,
                request.getRequestURI(),
                request.getMethod(),
                request.getContentType(),
                rawBody
        );
    }
}
