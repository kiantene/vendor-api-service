package com.nextgen.gameaggregator.vendor.spribe.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.entity.AgentPlayer;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.AgentPlayerDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.vendor.routing.VendorRouteContext;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpribeRouteResolverTest {

    @Mock
    private AgentPlayerDataService agentPlayerDataService;
    @Mock
    private VendorPlayerDataService vendorPlayerDataService;
    @Mock
    private GameTransactionService gameTransactionService;
    @Mock
    private SpribeConfig spribeConfig;

    private SpribeRouteResolver resolver;

    private static final String VALID_BODY = """
            {
              "user_id": "u1",
              "withdraw_provider_tx_id": "tx123"
            }
            """;

    @BeforeEach
    void setUp() {
        resolver = new SpribeRouteResolver(
                new ObjectMapper(),
                agentPlayerDataService,
                vendorPlayerDataService,
                gameTransactionService,
                spribeConfig
        );
    }

    // ---------------- Guard Clauses ----------------

    @Test
    void shouldReturnEmpty_whenContentTypeNotJson() {
        Optional<String> result = resolver.resolveTargetUri(
                contextWithContentType("text/plain", VALID_BODY)
        );
        assertThat(result).isEmpty();
        verifyPlayerLookupSkipped();
    }

    @Test
    void shouldReturnEmpty_whenRawBodyBlank() {
        Optional<String> result = resolver.resolveTargetUri(
                contextWithContentType("application/json", " ")
        );
        assertThat(result).isEmpty();
        verifyPlayerLookupSkipped();
    }

    @Test
    void shouldReturnEmpty_whenUserIdMissing() {
        String body = "{\"withdraw_provider_tx_id\": \"tx123\"}";
        Optional<String> result = resolver.resolveTargetUri(
                contextWithContentType("application/json", body)
        );
        assertThat(result).isEmpty();
        verifyPlayerLookupSkipped();
    }

    @Test
    void shouldReturnEmpty_whenJsonInvalid() {
        Optional<String> result = resolver.resolveTargetUri(
                contextWithContentType("application/json", "{invalid-json")
        );
        assertThat(result).isEmpty();
        verifyPlayerLookupSkipped();
    }

    // ---------------- Normal Routing (not special endpoint) ----------------

    @Test
    void shouldRouteViaV1ToV2_whenNotSpecialEndpoint() {
        mockAgentLookup();
        when(spribeConfig.getRoutingEndPoints()).thenReturn(Set.of()); // empty → not special

        Optional<String> result = resolver.resolveTargetUri(
                contextWithOriginalUri("/withdraw", VALID_BODY)
        );

        // Not a special endpoint → routed via the v1→v2 resolver, never the txn store.
        assertThat(result).isInstanceOf(Optional.class);
        verify(gameTransactionService, never()).get(any(String.class));
    }

    // ---------------- Special Routing ----------------

    @Test
    void shouldReturnEmpty_whenGameTransactionNotFound() {
        mockAgentLookup();
        when(spribeConfig.getRoutingEndPoints()).thenReturn(Set.of("/withdraw"));
        when(spribeConfig.getVendorClassName()).thenReturn("VENDOR");

        when(gameTransactionService.get("VENDOR::BET::tx123")).thenReturn(Optional.empty());

        Optional<String> result = resolver.resolveTargetUri(
                contextWithOriginalUri("/withdraw", VALID_BODY)
        );

        // Legacy-framework bet (no txn) → not routed to v2.
        assertThat(result).isEmpty();
        verify(gameTransactionService).get("VENDOR::BET::tx123");
    }

    @Test
    void shouldRouteViaV1ToV2_whenGameTransactionFound() {
        mockAgentLookup();
        when(spribeConfig.getRoutingEndPoints()).thenReturn(Set.of("/withdraw"));
        when(spribeConfig.getVendorClassName()).thenReturn("VENDOR");

        when(gameTransactionService.get("VENDOR::BET::tx123")).thenReturn(Optional.of(mock(GameTransaction.class)));

        Optional<String> result = resolver.resolveTargetUri(
                contextWithOriginalUri("/withdraw", VALID_BODY)
        );

        // New-framework bet (txn present) → delegated to the v1→v2 resolver.
        assertThat(result).isInstanceOf(Optional.class);
        verify(gameTransactionService).get("VENDOR::BET::tx123");
    }

    // ---------------- Freebet Routing ----------------

    @Test
    void shouldBypassGameTransactionCheck_whenActionIsFreebetAndVendorBetIdAbsent() {
        mockAgentLookup(3);
        when(spribeConfig.getRoutingEndPoints()).thenReturn(Set.of("/deposit"));

        String freebetBody = """
                {
                  "user_id": "u1",
                  "action": "freebet"
                }
                """;

        resolver.resolveTargetUri(contextWithOriginalUri("/deposit", freebetBody));

        // Route resolver must not consult game transaction store for freebet deposits.
        verify(gameTransactionService, never()).get(any(String.class));
    }

    @Test
    void shouldReturnEmpty_whenNoVendorBetIdAndActionIsNotFreebet() {
        mockAgentLookup(3);
        when(spribeConfig.getRoutingEndPoints()).thenReturn(Set.of("/deposit"));

        String unknownBody = """
                {
                  "user_id": "u1",
                  "action": "unknownaction"
                }
                """;

        Optional<String> result = resolver.resolveTargetUri(contextWithOriginalUri("/deposit", unknownBody));

        assertThat(result).isEmpty();
        verify(gameTransactionService, never()).get(any(String.class));
    }

    // ---------------- Exception Handling ----------------

    @Test
    void shouldReturnEmpty_whenVendorPlayerLookupThrows() {
        when(vendorPlayerDataService.getByUsername("u1"))
                .thenThrow(new RuntimeException("DB down"));

        Optional<String> result = resolver.resolveTargetUri(
                contextWithOriginalUri("/withdraw", VALID_BODY)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenGameTransactionServiceThrows() {
        mockAgentLookup();
        when(spribeConfig.getRoutingEndPoints()).thenReturn(Set.of("/withdraw"));
        when(spribeConfig.getVendorClassName()).thenReturn("VENDOR");

        when(gameTransactionService.get((String) any()))
                .thenThrow(new RuntimeException("CB down"));

        Optional<String> result = resolver.resolveTargetUri(
                contextWithOriginalUri("/withdraw", VALID_BODY)
        );

        assertThat(result).isEmpty();
        verify(gameTransactionService).get("VENDOR::BET::tx123");
    }

    // ---------------- Helpers ----------------

    private VendorRouteContext contextWithContentType(String contentType, String rawBody) {
        return new VendorRouteContext(
                spribeConfig,
                "/withdraw",
                "POST",
                contentType,
                rawBody
        );
    }

    private VendorRouteContext contextWithOriginalUri(String originalUri, String rawBody) {
        return new VendorRouteContext(
                spribeConfig,
                originalUri,
                "POST",
                "application/json",
                rawBody
        );
    }

    private void mockAgentLookup() {
        VendorPlayer vendorPlayer = mock(VendorPlayer.class);
        AgentPlayer agentPlayer = mock(AgentPlayer.class);

        when(vendorPlayer.getAgentPlayerId()).thenReturn(10L);

        when(vendorPlayerDataService.getByUsername("u1")).thenReturn(vendorPlayer);
        when(agentPlayerDataService.get(10L)).thenReturn(agentPlayer);
    }

    // Added due to conflict with other branchs
    @Deprecated
    private void mockAgentLookup(int version) {
        mockAgentLookup();
    }

    private void verifyPlayerLookupSkipped() {
        verify(vendorPlayerDataService, never()).getByUsername(any());
        verify(gameTransactionService, never()).get(any(String.class));
    }
}
