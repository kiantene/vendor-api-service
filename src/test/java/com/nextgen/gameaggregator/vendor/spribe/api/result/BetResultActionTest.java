package com.nextgen.gameaggregator.vendor.spribe.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.BetResultController;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.BetResultRequestMapper;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.BetResultResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.api.v2.result.FreebetDepositHandler;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.util.List;

import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BetResultActionTest {

    @Mock private BetResultRequestMapper requestMapper;
    @Mock private BetResultResponseMapper responseMapper;
    @Mock private WalletBetResultServiceWrapper walletService;
    @Mock private FreebetDepositHandler freebetHandler;

    private BetResultController controller;

    @BeforeEach
    void setUp() {
        controller = new BetResultController(requestMapper, responseMapper, walletService, List.of(freebetHandler));
    }

    private BetResultRequest buildRequest(String action) {
        BetResultRequest req = new BetResultRequest();
        req.setUserId("player_001");
        req.setSessionToken("tok-valid");
        req.setCurrency("USD");
        req.setAmount(new BigDecimal("5320"));
        req.setGame("aviator");
        req.setActionId("FB001");
        req.setAction(action);
        req.setProvider("spribe");
        req.setProviderTxId("spribe-tx-00001");
        return req;
    }

    // -----------------------------------------------------------------------
    // Freebet Dispatch (Scenario 1.1)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Freebet Dispatch (Scenario 1.1)")
    class FreebetDispatch {

        // Scenario 1.1
        @Test
        @DisplayName("1.1: action=freebet routes to FreebetDepositHandler; walletService never called")
        void freebetAction_dispatchesToFreebetHandler() {
            BetResultRequest request = buildRequest("freebet");
            SuccessResponse.Data data = SuccessResponse.Data.builder()
                    .userId("player_001").currency("USD")
                    .newBalance(new BigDecimal("15320")).build();
            SuccessResponse expectedResponse = new SuccessResponse(data);

            when(freebetHandler.supports("freebet")).thenReturn(true);
            when(freebetHandler.handle(request)).thenReturn(expectedResponse);

            ResponseEntity<SuccessResponse> result = controller.result(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(expectedResponse);
            verify(freebetHandler).handle(request);
            verifyNoInteractions(walletService);
        }
    }

    // -----------------------------------------------------------------------
    // Regular Bet Path — Regression (Scenarios 2.1, 2.2)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Regular Bet Path — Regression (Scenarios 2.1, 2.2)")
    class RegularBetPath {

        private PlayerBalanceData balanceData() {
            return new PlayerBalanceData("player_001", "USD", new BigDecimal("15.32"), System.currentTimeMillis());
        }

        private SuccessResponse stubResponse() {
            SuccessResponse.Data data = SuccessResponse.Data.builder()
                    .userId("player_001").currency("USD")
                    .newBalance(new BigDecimal("15320")).build();
            return new SuccessResponse(data);
        }

        private void stubWalletPath(PlayerBalanceData balanceData, SuccessResponse response) {
            BetResultContext ctx = BetResultContext.builder()
                    .vendorPlayerUsername("player_001")
                    .vendorCurrency("USD")
                    .build();
            when(requestMapper.toInternal(any())).thenReturn(ctx);
            when(walletService.initialise(any())).thenReturn(walletService);
            when(walletService.configure(any())).thenReturn(walletService);
            when(walletService.process()).thenReturn(balanceData);
            when(responseMapper.toVendor(any(), eq(balanceData))).thenReturn(response);
        }

        // Scenario 2.1
        @Test
        @DisplayName("2.1: action=bet — no handler matches, falls through to processRequest; FreebetDepositHandler never called")
        void betAction_noHandlerMatch_fallsThroughToProcessRequest() {
            BetResultRequest request = buildRequest("bet");
            PlayerBalanceData balanceData = balanceData();
            SuccessResponse response = stubResponse();

            when(freebetHandler.supports("bet")).thenReturn(false);
            stubWalletPath(balanceData, response);

            ResponseEntity<SuccessResponse> result = controller.result(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(walletService).initialise(any());
            verify(freebetHandler, never()).handle(any());
        }

        // Scenario 2.2 — out-of-scope action (promofreebet) not handled → regular wallet path
        @Test
        @DisplayName("2.2: action=promofreebet — no handler matches, falls through to processRequest; FreebetDepositHandler never called")
        void promofreebet_noHandlerMatch_fallsThroughToProcessRequest() {
            BetResultRequest request = buildRequest("promofreebet");
            PlayerBalanceData balanceData = balanceData();
            SuccessResponse response = stubResponse();

            when(freebetHandler.supports("promofreebet")).thenReturn(false);
            stubWalletPath(balanceData, response);

            ResponseEntity<SuccessResponse> result = controller.result(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(walletService).initialise(any());
            verify(freebetHandler, never()).handle(any());
        }
    }

    // -----------------------------------------------------------------------
    // Invalid Session Token (Scenario 3.1)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Invalid Session Token (Scenario 3.1)")
    class InvalidSessionToken {

        // Scenario 3.1
        @Test
        @DisplayName("3.1: invalid session_token — GameTerminatedException propagates; no balance change")
        void invalidSessionToken_walletServiceThrows_exceptionPropagates() {
            BetResultRequest request = buildRequest("bet");
            BetResultContext ctx = BetResultContext.builder()
                    .vendorPlayerUsername("player_001")
                    .vendorCurrency("USD")
                    .build();

            when(freebetHandler.supports("bet")).thenReturn(false);
            when(requestMapper.toInternal(any())).thenReturn(ctx);
            when(walletService.initialise(any())).thenReturn(walletService);
            when(walletService.configure(any())).thenReturn(walletService);
            when(walletService.process()).thenThrow(new GameTerminatedException());

            assertThatThrownBy(() -> controller.result(request))
                    .isInstanceOf(GameTerminatedException.class);
            verify(freebetHandler, never()).handle(any());
        }
    }

    // -----------------------------------------------------------------------
    // Expired Session Token (Scenario 3.2)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Expired Session Token (Scenario 3.2)")
    class ExpiredSessionToken {

        // Scenario 3.2
        @Test
        @DisplayName("3.2: expired session_token — GameSessionExpiredException propagates; no balance change")
        void expiredSessionToken_walletServiceThrows_exceptionPropagates() {
            BetResultRequest request = buildRequest("bet");
            BetResultContext ctx = BetResultContext.builder()
                    .vendorPlayerUsername("player_001")
                    .vendorCurrency("USD")
                    .build();

            when(freebetHandler.supports("bet")).thenReturn(false);
            when(requestMapper.toInternal(any())).thenReturn(ctx);
            when(walletService.initialise(any())).thenReturn(walletService);
            when(walletService.configure(any())).thenReturn(walletService);
            when(walletService.process()).thenThrow(new GameSessionExpiredException());

            assertThatThrownBy(() -> controller.result(request))
                    .isInstanceOf(GameSessionExpiredException.class);
            verify(freebetHandler, never()).handle(any());
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 3.3 — Invalid X-Spribe-Client-Signature — tested in SignatureValidationTest.java
    // The 413 path fires in SpribeSignatureValidator before BetResultController is reached;
    // it cannot be exercised as a controller unit test.
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Missing Required Fields (Scenario 3.4)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Missing Required Fields (Scenario 3.4)")
    class MissingRequiredFields {

        // Scenario 3.4
        @Test
        @DisplayName("3.4: provider_tx_id absent — @NotBlank validation fails on BetResultRequest")
        void missingProviderTxId_validationFails_returns500() {
            BetResultRequest request = buildRequest("freebet");
            request.setProviderTxId(null);

            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            var violations = validator.validate(request);

            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("providerTxId");
        }
    }
}
