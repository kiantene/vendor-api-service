package com.nextgen.gameaggregator.core.vendor.routing;

import java.util.Optional;

/**
 * Default {@link VendorCallbackRouteResolver} that rewrites v1 vendor callback
 * paths to their v2 equivalents.
 *
 * <p>Rule:
 * {@code /api/v1/{vendor}/{action...} -> /api/v1/{vendor}/v2/{action...}}</p>
 *
 * <p>Safeguards:
 * <ul>
 *   <li>Does not route paths that already contain {@code /v2/}</li>
 *   <li>Only applies to {@code /api/v1/} callback paths</li>
 * </ul>
 *
 * <p>This resolver performs path transformation only and does not inspect
 * request body or headers.</p>
 */
public class DefaultV1ToV2RouteResolver implements VendorCallbackRouteResolver {

    @Override
    public Optional<String> resolveTargetUri(VendorRouteContext ctx) {
        String uri = ctx.originalUri();
        if (uri == null || uri.isBlank()) return Optional.empty();

        // Prevent loops
        if (uri.contains("/v2/")) return Optional.empty();

        // Only handle /api/v1/{vendor}/{rest...}
        String prefix = "/api/v1/";
        if (!uri.startsWith(prefix)) return Optional.empty();

        String remaining = uri.substring(prefix.length()); // {vendor}/{rest...}
        int firstSlash = remaining.indexOf('/');
        if (firstSlash <= 0) return Optional.empty(); // no vendor or no rest

        String vendor = remaining.substring(0, firstSlash);
        String rest = remaining.substring(firstSlash + 1);

        if (rest.isBlank()) return Optional.empty();

        return Optional.of("/api/v1/" + vendor + "/v2/" + rest);
    }
}
