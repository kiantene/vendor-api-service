package com.nextgen.gameaggregator.vendor.spribe.filter;

import com.nextgen.gameaggregator.core.common.RequestAttributes;
import com.nextgen.gameaggregator.core.common.RequestParserService;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapperRegistry;
import com.nextgen.gameaggregator.core.filter.VendorAuthFilter;
import com.nextgen.gameaggregator.core.filter.VendorCallbackRoutingFilter;
import com.nextgen.gameaggregator.core.security.VendorSecurityAdapter;
import com.nextgen.gameaggregator.core.security.VendorSecurityRegistry;
import com.nextgen.gameaggregator.core.security.decrypter.VendorDecryptionService;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureService;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.core.vendor.config.VendorIntegrationConfig;
import com.nextgen.gameaggregator.core.vendor.routing.VendorCallbackRouteResolver;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies the per-endpoint filter chain behavior documented in:
 * docs/vendors/spribe/implementation/gap-signature-validation-filter-chain.md
 *
 * Each test maps to one row in the gap document's per-endpoint table.
 * The key proxy assertion: whether signatureService.doValidation() is invoked,
 * which means VendorAuthFilter ran and found a registered validator.
 *
 * Scope and limitations:
 *   - routeResolver is mocked — tests the filter plumbing, not SpribeRouteResolver DB logic.
 *   - handlerMapping is mocked — tests hasHandlerFor() branching, not real Spring MVC registration.
 *   - Both are separately verified by reading the production code; see gap doc for details.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpribeFilterChainBehaviorTest {

    private static final String SPRIBE = "spribe";
    private static final String BODY   = "{\"user_id\":\"player_001\"}";

    // --- routing filter ---
    @Mock private VendorCallbackRouteResolver    routeResolver;
    @Mock private RequestMappingHandlerMapping   handlerMapping;
    @Mock private VendorConfigService            vendorConfigService;
    @Mock private VendorIntegrationConfig        spribeConfig;

    // --- auth filter ---
    @Mock private RequestParserService           parserService;
    @Mock private VendorSecurityRegistry         securityRegistry;
    @Mock private VendorSecurityAdapter          securityAdapter;
    @Mock private VendorSignatureValidator       signatureValidator;
    @Mock private VendorDecryptionService        decryptionService;
    @Mock private VendorSignatureService         signatureService;
    @Mock private VendorExceptionMapperRegistry  exceptionRegistry;

    private VendorCallbackRoutingFilter routingFilter;
    private VendorAuthFilter            authFilter;

    @BeforeEach
    void setUp() throws Exception {
        // SpribeConfig: callback routing enabled, validator registered
        when(spribeConfig.isCallbackRoutingEnabled()).thenReturn(true);
        when(spribeConfig.callbackRouteResolver()).thenReturn(Optional.of(routeResolver));
        when(spribeConfig.isNewFramework()).thenReturn(true);
        when(spribeConfig.getVendorClassName()).thenReturn(SPRIBE);

        when(vendorConfigService.isCallbackRoutingEnabled(SPRIBE)).thenReturn(true);
        when(vendorConfigService.getVendorIntegrationConfig(SPRIBE)).thenReturn(Optional.of(spribeConfig));
        when(vendorConfigService.getConfigByRequestURI(anyString())).thenReturn(Optional.of(spribeConfig));

        when(securityRegistry.get(SPRIBE)).thenReturn(securityAdapter);
        when(securityAdapter.validator()).thenReturn(Optional.of(signatureValidator));
        when(securityAdapter.decrypter()).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(Map.of());
        when(signatureService.doValidation(any(), any(), any(), any())).thenReturn(ValidationResult.success());

        // hasHandlerFor() stubs — mirrors actual v2 controller registrations:
        //   BetController    → api/v1/spribe/v2/withdraw  (handler EXISTS)
        //   BetResultController → api/v1/spribe/v2/deposit (handler EXISTS)
        //   RollbackController  → api/v1/spribe/v2/rollback (handler EXISTS)
        //   AuthController      → api/v1/spribe/auth       (NO /v2/auth handler)
        //   BalanceController   → api/v1/spribe/info       (NO /v2/info handler)
        doReturn(mock(HandlerExecutionChain.class))
                .when(handlerMapping).getHandler(
                        argThat(req -> req.getRequestURI().matches(".*/(withdraw|deposit|rollback)")));
        doReturn(null)
                .when(handlerMapping).getHandler(
                        argThat(req -> req.getRequestURI().matches(".*/(auth|info)")));

        routingFilter = new VendorCallbackRoutingFilter(List.of(handlerMapping), vendorConfigService);
        authFilter    = new VendorAuthFilter(parserService, securityRegistry, decryptionService,
                                             signatureService, exceptionRegistry, vendorConfigService);
    }

    /**
     * Chains routing → auth → terminal, matching the production filter order.
     * When routing calls chain.doFilter(), auth runs.
     * When routing calls forward(), the MockRequestDispatcher records it and returns — chain is never called.
     */
    private FilterChain buildChain() {
        return (req, res) -> authFilter.doFilter(req, res, (r2, r3) -> {});
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setContentType("application/json");
        req.setContent(BODY.getBytes(StandardCharsets.UTF_8));
        req.setAttribute(RequestAttributes.VENDOR_CLASS_NAME, SPRIBE);
        req.setAttribute(RequestAttributes.RAW_BODY, BODY);
        return req;
    }

    // -----------------------------------------------------------------------
    // /auth
    // AuthController is registered at api/v1/spribe/auth (no /v2/ suffix).
    // hasHandlerFor(/v2/auth) = false → routing always falls through.
    // VendorAuthFilter runs for all players on this endpoint.
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("/auth — no /v2/auth handler; VendorAuthFilter runs for all players")
    class AuthEndpoint {

        @Test
        @DisplayName("v2 player: resolver generates /v2/auth; hasHandlerFor=false → routing falls through → auth filter runs")
        void v2Player_noV2Handler_authFilterRuns() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.of("/api/v1/spribe/v2/auth"));

            routingFilter.doFilter(request("/api/v1/spribe/auth"), new MockHttpServletResponse(), buildChain());

            verify(signatureService).doValidation(any(), any(), any(), any());
        }

        @Test
        @DisplayName("v1 player: resolver returns empty → routing falls through → auth filter runs")
        void v1Player_resolverEmpty_authFilterRuns() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.empty());

            routingFilter.doFilter(request("/api/v1/spribe/auth"), new MockHttpServletResponse(), buildChain());

            verify(signatureService).doValidation(any(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // /info
    // BalanceController is registered at api/v1/spribe/info (no /v2/ suffix).
    // Same pattern as /auth.
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("/info — no /v2/info handler; VendorAuthFilter runs for all players")
    class InfoEndpoint {

        @Test
        @DisplayName("v2 player: resolver generates /v2/info; hasHandlerFor=false → routing falls through → auth filter runs")
        void v2Player_noV2Handler_authFilterRuns() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.of("/api/v1/spribe/v2/info"));

            routingFilter.doFilter(request("/api/v1/spribe/info"), new MockHttpServletResponse(), buildChain());

            verify(signatureService).doValidation(any(), any(), any(), any());
        }

        @Test
        @DisplayName("v1 player: resolver returns empty → routing falls through → auth filter runs")
        void v1Player_resolverEmpty_authFilterRuns() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.empty());

            routingFilter.doFilter(request("/api/v1/spribe/info"), new MockHttpServletResponse(), buildChain());

            verify(signatureService).doValidation(any(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // /deposit
    // BetResultController is registered at api/v1/spribe/v2/deposit.
    // hasHandlerFor(/v2/deposit) = true for v2 players → routing forwards → auth skipped.
    // v1 players: resolver returns empty → falls through → auth runs.
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("/deposit — routing forwards for v2 (auth skipped); falls through for v1")
    class DepositEndpoint {

        @Test
        @DisplayName("v2 player: resolver generates /v2/deposit; hasHandlerFor=true → routing forwards → auth filter NOT called")
        void v2Player_routingForwards_authFilterNotCalled() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.of("/api/v1/spribe/v2/deposit"));

            routingFilter.doFilter(request("/api/v1/spribe/deposit"), new MockHttpServletResponse(), buildChain());

            verify(signatureService, never()).doValidation(any(), any(), any(), any());
        }

        @Test
        @DisplayName("v1 player: resolver returns empty → routing falls through → auth filter runs")
        void v1Player_resolverEmpty_authFilterRuns() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.empty());

            routingFilter.doFilter(request("/api/v1/spribe/deposit"), new MockHttpServletResponse(), buildChain());

            verify(signatureService).doValidation(any(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // /withdraw
    // BetController is registered at api/v1/spribe/v2/withdraw.
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("/withdraw — routing forwards for v2 (auth skipped); falls through for v1")
    class WithdrawEndpoint {

        @Test
        @DisplayName("v2 player: resolver generates /v2/withdraw; hasHandlerFor=true → routing forwards → auth filter NOT called")
        void v2Player_routingForwards_authFilterNotCalled() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.of("/api/v1/spribe/v2/withdraw"));

            routingFilter.doFilter(request("/api/v1/spribe/withdraw"), new MockHttpServletResponse(), buildChain());

            verify(signatureService, never()).doValidation(any(), any(), any(), any());
        }

        @Test
        @DisplayName("v1 player: resolver returns empty → routing falls through → auth filter runs")
        void v1Player_resolverEmpty_authFilterRuns() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.empty());

            routingFilter.doFilter(request("/api/v1/spribe/withdraw"), new MockHttpServletResponse(), buildChain());

            verify(signatureService).doValidation(any(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // /rollback
    // RollbackController is registered at api/v1/spribe/v2/rollback.
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("/rollback — routing forwards for v2 (auth skipped); falls through for v1")
    class RollbackEndpoint {

        @Test
        @DisplayName("v2 player: resolver generates /v2/rollback; hasHandlerFor=true → routing forwards → auth filter NOT called")
        void v2Player_routingForwards_authFilterNotCalled() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.of("/api/v1/spribe/v2/rollback"));

            routingFilter.doFilter(request("/api/v1/spribe/rollback"), new MockHttpServletResponse(), buildChain());

            verify(signatureService, never()).doValidation(any(), any(), any(), any());
        }

        @Test
        @DisplayName("v1 player: resolver returns empty → routing falls through → auth filter runs")
        void v1Player_resolverEmpty_authFilterRuns() throws Exception {
            when(routeResolver.resolveTargetUri(any())).thenReturn(Optional.empty());

            routingFilter.doFilter(request("/api/v1/spribe/rollback"), new MockHttpServletResponse(), buildChain());

            verify(signatureService).doValidation(any(), any(), any(), any());
        }
    }
}
