package com.nextgen.gameaggregator.vendor.spribe.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.entity.AgentPlayer;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.vendor.routing.DefaultV1ToV2RouteResolver;
import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import com.nextgen.gameaggregator.core.vendor.routing.VendorRouteContext;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.AgentApiVersionService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class SpribeRouteResolver implements VendorCallbackRouteResolver {

    private final ObjectMapper objectMapper;
    private final VendorCallbackRouteResolver v1ToV2Resolver = new DefaultV1ToV2RouteResolver();
    private final AgentApiVersionService agentApiVersionService;
    private final AgentPlayerDataService agentPlayerDataService;
    private final VendorPlayerDataService vendorPlayerDataService;
    private final GameTransactionService gameTransactionService;
    private final SpribeConfig spribeConfig;

    public SpribeRouteResolver(ObjectMapper objectMapper,
                               AgentApiVersionService agentApiVersionService,
                               AgentPlayerDataService agentPlayerDataService,
                               VendorPlayerDataService vendorPlayerDataService,
                               GameTransactionService gameTransactionService,
                               SpribeConfig spribeConfig) {
        this.objectMapper = objectMapper;
        this.agentApiVersionService = agentApiVersionService;
        this.agentPlayerDataService = agentPlayerDataService;
        this.vendorPlayerDataService = vendorPlayerDataService;
        this.gameTransactionService = gameTransactionService;
        this.spribeConfig = spribeConfig;
    }

    @Override
    public Optional<String> resolveTargetUri(VendorRouteContext context) {
        // Only apply to JSON payloads
        String contentType = context.contentType();
        if (contentType == null || !contentType.contains("application/json")) {
            return Optional.empty();
        }

        String rawBody = context.rawBody();
        if (rawBody == null || rawBody.isBlank()) {
            return Optional.empty();
        }

        Optional<RequiredValues> requiredValues = extractRequiredValues(rawBody);
        // If any Key not available, it will return Empty
        if (requiredValues.isEmpty()) {
            return Optional.empty();
        }

        try {
            VendorPlayer vendorPlayer = vendorPlayerDataService.getByUsername(requiredValues.get().username);
            AgentPlayer agentPlayer = agentPlayerDataService.get(vendorPlayer.getAgentPlayerId());
            int apiVersion = agentApiVersionService.getAgentApiVersion(agentPlayer.getAgentId());

            if (apiVersion == 3) {
                if (needToCheckGameTransaction(context)) {
                    return resolveByGameTransaction(context, requiredValues.get());
                } else {
                    return v1ToV2Resolver.resolveTargetUri(context);
                }
            }
        } catch (Exception ex) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private boolean needToCheckGameTransaction(VendorRouteContext context) {
        Set<String> specialRoutingEndPoints = spribeConfig.getRoutingEndPoints();
        return specialRoutingEndPoints.contains(context.originalUri());
    }

    private Optional<String> resolveByGameTransaction(VendorRouteContext context, RequiredValues requiredValues) {

        if (requiredValues.vendorBetId.isEmpty()) {
            return Optional.empty();
        }

        String docId = GameTransaction.createDocId(spribeConfig.getVendorClassName(), TxnType.BET, requiredValues.vendorBetId.get());
        Optional<GameTransaction> txnOpt = gameTransactionService.get(docId);
        if (txnOpt.isPresent()) {
            // Bet was placed via New Framework
            return v1ToV2Resolver.resolveTargetUri(context);
        } else {
            // Bet was placed via Legacy Framework
            return Optional.empty();
        }
    }

    private Optional<RequiredValues> extractRequiredValues(String rawBody) {

        final String USER_NAME = "user_id";

        try {
            JsonNode root = objectMapper.readTree(rawBody);

            if (!root.hasNonNull(USER_NAME)) {
                return Optional.empty();
            }

            String username = root.get(USER_NAME).asText();

            Optional<String> vendorBetId = extractVendorBetId(root);

            return Optional.of(new RequiredValues(username, vendorBetId));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractVendorBetId(JsonNode root) {
        final String RESULT_VENDOR_BET_ID = "withdraw_provider_tx_id";
        final String ROLLBACK_VENDOR_BET_ID = "rollback_provider_tx_id";

        return Stream.of(RESULT_VENDOR_BET_ID,ROLLBACK_VENDOR_BET_ID)
                .filter(root::hasNonNull)
                .map(key -> root.get(key).asText())
                .findFirst();
    }

    private record RequiredValues(String username, Optional<String> vendorBetId) {}
}
