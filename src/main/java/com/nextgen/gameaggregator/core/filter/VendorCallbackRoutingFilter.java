package com.nextgen.gameaggregator.core.filter;

import com.nextgen.gameaggregator.core.common.RequestAttributes;
import com.nextgen.gameaggregator.core.vendor.config.VendorIntegrationConfig;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.core.vendor.routing.VendorRouteContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.List;

/**
 * Routes vendor callback requests to newer internal versions (e.g. v2)
 * based on resolved VendorConfig.
 *
 * This filter:
 * - runs after RequestLoggingFilter
 * - requires vendor context to be present
 * - performs internal forward (no redirect)
 * - only routes if a matching handler exists
 */
@Component
@RequiredArgsConstructor
public class VendorCallbackRoutingFilter extends OncePerRequestFilter {
    /**
     * All registered Spring MVC handler mappings.
     * Used to verify whether a controller exists for a routed URI
     * before performing an internal forward.
     */
    private final List<HandlerMapping> handlerMappings;
    private final VendorConfigService vendorConfigService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Vendor must already be resolved by RequestLoggingFilter
        String vendorClassName = (String) request.getAttribute(RequestAttributes.VENDOR_CLASS_NAME);
        if (vendorClassName == null || vendorClassName.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Load vendor configuration and check if callback routing is enabled
        if (!vendorConfigService.isCallbackRoutingEnabled(vendorClassName)) {
            filterChain.doFilter(request, response);
            return;
        }

        // No need check isPresent here. If not Present, would have failed previous condition
        VendorIntegrationConfig vendorConfig = vendorConfigService.getVendorIntegrationConfig(vendorClassName).get();

        // Ask vendor-specific resolver whether this request should be routed
        VendorRouteContext routeContext = VendorRouteContext.of(vendorConfig, request);
        var targetOpt = vendorConfig.callbackRouteResolver()
                .flatMap(r -> r.resolveTargetUri(routeContext));

        if (targetOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String targetUri = targetOpt.get();
        String originalUri = request.getRequestURI();

        // loop guards / sanity
        if (targetUri.isBlank() || targetUri.equals(originalUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Safety: only forward if a handler exists for the target route
        if (!hasHandlerFor(request, targetUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Useful for logging/audit
//        request.setAttribute(RequestAttributes.ORIGINAL_URI, originalUri);
//        request.setAttribute(RequestAttributes.ROUTED_URI, targetUri);

        // Internal forward to the routed URI (no client-visible redirect)
        request.getRequestDispatcher(targetUri).forward(request, response);
    }

    /**
     * Checks whether Spring MVC can resolve a controller handler
     * for the given target URI.
     *
     * <p>This mirrors DispatcherServlet behavior by delegating
     * to all registered {@link HandlerMapping}s.</p>
     *
     * @return true if any handler mapping resolves a handler for the target URI
     */
    private boolean hasHandlerFor(HttpServletRequest original, String targetUri) {
        HttpServletRequest wrapped = new HttpServletRequestWrapper(original) {
            @Override public String getRequestURI() { return targetUri; }
            @Override public String getServletPath() { return targetUri; }
        };

        for (HandlerMapping hm : handlerMappings) {
            // Only check RequestMappingHandlerMapping (actual @RequestMapping endpoints)
            if (hm instanceof RequestMappingHandlerMapping) {
                try {
                    var handler = hm.getHandler(wrapped);
                    if (handler != null) {
                        return true;
                    }
                } catch (Exception ignored) {
                    // ignore and continue; we want "true" only if a mapping confirms it
                }
            }
        }
        return false;
    }
}
