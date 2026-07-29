package com.nextgen.gameaggregator.vendor.evoplay.config;

import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.core.vendor.routing.VendorRouteContext;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ActionName;
import lombok.RequiredArgsConstructor;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evoplay Route Resolver {@link VendorCallbackRouteResolver} that rewrites v1 callback
 * paths to their v2 equivalents.
 *
 * <p>Rule:
 * {@code /api/v1/evoplay} -> /api/v1/evoplay/v2}</p>
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
@RequiredArgsConstructor
public class EvoplayRouteResolver implements VendorCallbackRouteResolver {

    private final EvoplayConfig evoplayConfig;
    private final MigrationRoundDataService migrationRoundDataService;

    @Override
    public Optional<String> resolveTargetUri(VendorRouteContext ctx) {
        String uri = ctx.originalUri();
        if (uri == null || uri.isBlank()) return Optional.empty();

        // Prevent loops
        if (uri.contains("/v2/")) return Optional.empty();

        // Only handle /api/v1/{vendor}/{rest...}
        String prefix = "/api/v1/";
        if (!uri.startsWith(prefix)) return Optional.empty();

        // If any Txn of the Round happened in v1, all subsequent txn should go v1
        if (shouldRouteToV1(ctx)) return Optional.empty();

        return Optional.of(uri + "/v2");
    }

    private boolean shouldRouteToV1(VendorRouteContext context) {
        EvoplayCallback callback = parse(context.rawBody());

        Set<String> specialRoutingActions = ActionName.getActionNamesRelevantForRouting();

        return specialRoutingActions.contains(callback.name) &&
                migrationRoundDataService.findMarker(evoplayConfig.getVendorClassName(), callback.roundId).isPresent();
    }

    private EvoplayCallback parse(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            throw new IllegalArgumentException("Empty body");
        }

        Map<String, String> params = parseFormUrlEncoded(rawBody);

        String name = params.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Missing required field: name");
        }
        name = name.toLowerCase(Locale.ROOT);

        String roundId = params.get("data[round_id]");
        if (ActionName.getActionNamesRelevantForRouting().contains(name)
                && (roundId == null || roundId.isBlank())) {
            throw new IllegalArgumentException(
                    "Missing required field data[round_id] for action name=" + name);
        }

        return new EvoplayCallback(name, roundId);
    }

    private Map<String, String> parseFormUrlEncoded(String body) {
        List<NameValuePair> pairs = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
        Map<String, String> params = pairs.stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (a, b) -> a));

        return params;
    }

    private record EvoplayCallback(String name, String roundId) {}
}
