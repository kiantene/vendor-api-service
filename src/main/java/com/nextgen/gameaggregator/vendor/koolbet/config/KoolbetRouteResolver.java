package com.nextgen.gameaggregator.vendor.koolbet.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.core.vendor.routing.VendorRouteContext;
import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class KoolbetRouteResolver implements VendorCallbackRouteResolver {

    private final KoolbetConfig koolbetConfig;
    private final MigrationRoundDataService migrationRoundDataService;
    private final ObjectMapper objectMapper;

    private record KoolbetCallback(String sessionId, String round) {}

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
        String currentUri = context.originalUri();
        if (currentUri == null) return false;

        KoolbetCallback callback = parse(context.rawBody());

        // Define endpoints requiring the "round" identifier
        Set<String> roundBasedEndPoints = Set.of(
                "/" + EndPoints.PATH + EndPoints.BET,
                "/" + EndPoints.PATH + EndPoints.CANCEL_BET
        );

        // Define endpoints requiring the "sessionId" identifier
        Set<String> sessionBasedEndPoints = Set.of(
                "/" + EndPoints.PATH + EndPoints.SESSION_BET,
                "/" + EndPoints.PATH + EndPoints.CANCEL_SESSION_BET
        );

        String markerKey = null;

        // 2. Select the key dynamically depending on the current request path
        if (roundBasedEndPoints.contains(currentUri)) {
            markerKey = callback.round();
        } else if (sessionBasedEndPoints.contains(currentUri)) {
            markerKey = callback.sessionId();
        }

        // If the path didn't match our criteria, or the field was absent in the payload
        if (markerKey == null || markerKey.isBlank()) {
            return false;
        }

        return migrationRoundDataService.findMarker(
                koolbetConfig.getVendorClassName(),
                markerKey
        ).isPresent();
    }

    private KoolbetCallback parse(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            throw new IllegalArgumentException("Empty body");
        }

        try {
            // Read JSON into a generic JsonNode tree
            JsonNode rootNode = objectMapper.readTree(rawBody);

            // Extract sessionId & round field safely
            JsonNode sessionIdNode = rootNode.get("sessionId");
            String sessionId = (sessionIdNode != null && !sessionIdNode.isNull())
                    ? sessionIdNode.asText()
                    : null;

            JsonNode roundNode = rootNode.get("round");
            String round = (roundNode != null && !roundNode.isNull())
                    ? roundNode.asText()
                    : null;

            return new KoolbetCallback(sessionId, round);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format payload provided", e);
        }
    }
}
