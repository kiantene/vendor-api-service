package com.nextgen.gameaggregator.vendor.jili.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RequestIdempotentLog;
import com.nextgen.gameaggregator.entity.ga.WalletTransaction;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.jili.api.freespin.JiliFreeSpinPayoutProcessor;
import com.nextgen.gameaggregator.vendor.jili.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BetAction#betRequest(HttpServletRequest)}.
 *
 * Scenario IDs (e.g. "2.1") reference docs/vendors/jili/functional-scenarios.md.
 * Tests covering the free-spin fork (scenarios 1.x, 5.1–5.5, 5.8) are deferred
 * until JiliFreeSpinPayoutHandler is implemented.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BetActionTest {

    @InjectMocks
    private BetAction betAction;

    @Mock private HttpService                 httpService;
    @Mock private GameSessionService          gameSessionService;
    @Mock private WalletService               walletService;
    @Mock private VendorService               vendorService;
    @Mock private ValidationService           validationService;
    @Mock private RequestIdempotentLogService requestIdempotentLogService;
    @Mock private JiliFreeSpinPayoutProcessor  jiliFreeSpinPayoutProcessor;

    @Mock private HttpRequestLog      httpRequestLog;
    @Mock private HttpServletRequest  httpServletRequest;

    // -----------------------------------------------------------------------
    // JSON request fixtures
    // -----------------------------------------------------------------------

    // Base valid request — betAmount=10, winloseAmount=25, no freeSpinData
    private static final String WIN_JSON = """
            {"reqId":"r-001","token":"tok-abc","currency":"THB",\
            "game":110,"round":17238050501001102,"wagersTime":1592559162,\
            "betAmount":10,"winloseAmount":25}""";

    // Standard loss — winloseAmount=0
    private static final String LOSE_JSON =
            WIN_JSON.replace("\"winloseAmount\":25", "\"winloseAmount\":0");

    // isFreeRound=true + transactionId (offline payment path — scenario 3.1)
    private static final String FREE_ROUND_JSON = """
            {"reqId":"r-004","token":"tok-abc","currency":"THB",\
            "game":110,"round":17238050501001104,"wagersTime":1592559162,\
            "betAmount":0,"winloseAmount":55,\
            "isFreeRound":true,"transactionId":1630891368000155009}""";

    // freeSpinData present — routes to JiliFreeSpinPayoutHandler (scenario 1.1)
    private static final String FREE_SPIN_JSON = """
            {"reqId":"abc-001","token":"tok-abc","currency":"THB",\
            "game":110,"round":17238050501001102001,"wagersTime":1592559162,\
            "betAmount":0,"winloseAmount":55.00,\
            "freeSpinData":{"referenceId":"FS001","remain":9,"originalBet":0.5,"deduct":1}}""";

    // freeSpinData with winloseAmount=0 — zero-win round (scenario 1.2)
    private static final String FREE_SPIN_ZERO_WIN_JSON =
            FREE_SPIN_JSON.replace("\"winloseAmount\":55.00", "\"winloseAmount\":0");

    // freeSpinData with remain=0 — last round consumed (scenario 1.3)
    private static final String FREE_SPIN_LAST_ROUND_JSON = """
            {"reqId":"abc-003","token":"tok-abc","currency":"THB",\
            "game":110,"round":17238050501001102003,"wagersTime":1592559162,\
            "betAmount":0,"winloseAmount":30.00,\
            "freeSpinData":{"referenceId":"FS001","remain":0,"originalBet":0.5,"deduct":1}}""";

    // Blank reqId — triggers @NotBlank violation
    private static final String BLANK_REQ_ID_JSON =
            WIN_JSON.replace("\"r-001\"", "\"\"");

    // Blank token — triggers @NotBlank violation
    private static final String BLANK_TOKEN_JSON =
            WIN_JSON.replace("\"tok-abc\"", "\"\"");

    // Malformed JSON — triggers JsonProcessingException
    private static final String MALFORMED_JSON = "{not valid json}";

    // betAmount=0 AND winloseAmount=0 — the second BET_WIN branch in getResultType
    private static final String ZERO_ZERO_JSON =
            WIN_JSON.replace("\"betAmount\":10", "\"betAmount\":0")
                    .replace("\"winloseAmount\":25", "\"winloseAmount\":0");

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    /**
     * Stubs httpService so BetAction reads the given JSON body.
     * HttpService.convertJsonToDto is a plain static Jackson call — no mock needed.
     */
    private void givenBody(String json) {
        when(httpService.start(httpServletRequest)).thenReturn(httpRequestLog);
        when(httpRequestLog.getId()).thenReturn("trace-test-001");
        when(httpRequestLog.getRequestBody()).thenReturn(json);
    }

    /** Builds a GameSession with the given gameCode and currency. */
    private GameSession session(String gameCode, String currency) {
        GameSession gs = new GameSession();
        gs.setVendorPlayerUsername("player001");
        gs.setVendorGameCode(gameCode);
        gs.setVendorCurrencyCode(currency);
        gs.setToken("tok-abc");
        gs.setStatus(1); // ACTIVE
        return gs;
    }

    /**
     * Stubs the "golden path" through verifyToken → idempotency check →
     * verifyAndRegenerateNewVendorGameCodeForGameSession.
     * Void methods (validateEligibleBet, setOperatorTiming) use Mockito's default no-op.
     */
    private void givenValidSession() throws Exception {
        GameSession gs = session("110", "THB");
        when(gameSessionService.verifyToken("tok-abc")).thenReturn(gs);
        when(requestIdempotentLogService.checkExists(any(BetResultData.class), eq("player001"))).thenReturn(null);
        when(vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(eq("110"), any()))
                .thenReturn(gs);
    }

    // -----------------------------------------------------------------------
    // Happy Path — Regular Bet (Scenarios 2.1, 2.2, 2.3)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Happy Path — Regular Bet (Scenarios 2.1, 2.2, 2.3)")
    class HappyPath {

        @Test
        // Scenario 2.1 — standard win, no freeSpinData, routes through walletService
        @DisplayName("2.1: win bet routes to walletService and returns updated balance")
        void regularBet_win_routesToWalletService() throws Exception {
            givenBody(WIN_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("1025.00"));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            assertEquals("player001", result.getUsername());
            assertEquals("THB", result.getCurrency());
            assertEquals(new BigDecimal("1025.00"), result.getBalance());
            assertEquals("tok-abc", result.getToken());
            ArgumentCaptor<ResultType> resultTypeCaptor = ArgumentCaptor.forClass(ResultType.class);
            verify(walletService).processBetResult(any(), any(), any(), resultTypeCaptor.capture(), any(), any());
            assertEquals(ResultType.BET_WIN, resultTypeCaptor.getValue());
        }

        @Test
        // Scenario 2.2 — standard loss, no freeSpinData
        @DisplayName("2.2: loss bet routes to walletService and returns debited balance")
        void regularBet_loss_routesToWalletService() throws Exception {
            givenBody(LOSE_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("990.00"));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            assertEquals(new BigDecimal("990.00"), result.getBalance());
            ArgumentCaptor<ResultType> resultTypeCaptor = ArgumentCaptor.forClass(ResultType.class);
            verify(walletService).processBetResult(any(), any(), any(), resultTypeCaptor.capture(), any(), any());
            assertEquals(ResultType.BET_LOSE, resultTypeCaptor.getValue());
        }

        @Test
        // Scenario 2.3 — freeSpinData absent/null in JSON body; treated as a regular bet
        @DisplayName("2.3: absent freeSpinData field routes to walletService (not free-spin handler)")
        void regularBet_noFreeSpinDataField_routesToWalletService() throws Exception {
            // WIN_JSON has no freeSpinData — same behaviour as explicit null
            givenBody(WIN_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("1000.00"));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            verify(walletService).processBetResult(any(), any(), any(), any(), any(), any());
        }

        @Test
        // No scenario ID — pins the second BET_WIN branch in getResultType:
        // winloseAmount=0 AND betAmount=0 (e.g. zero-cost offline payout) must not route as BET_LOSE
        @DisplayName("zero winloseAmount and zero betAmount → BET_WIN (not BET_LOSE)")
        void zeroBetAndZeroWin_routesAsBetWin() throws Exception {
            givenBody(ZERO_ZERO_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("1000.00"));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            ArgumentCaptor<ResultType> resultTypeCaptor = ArgumentCaptor.forClass(ResultType.class);
            verify(walletService).processBetResult(any(), any(), any(), resultTypeCaptor.capture(), any(), any());
            assertEquals(ResultType.BET_WIN, resultTypeCaptor.getValue());
        }
    }

    // -----------------------------------------------------------------------
    // Offline Payment — isFreeRound (Scenario 3.1)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Offline Payment — isFreeRound (Scenario 3.1)")
    class OfflinePayment {

        @Test
        // Scenario 3.1 — isFreeRound:true with transactionId; BetDto.getRoundId() returns transactionId
        @DisplayName("3.1: isFreeRound=true with transactionId succeeds via walletService")
        void isFreeRound_true_withTransactionId_succeeds() throws Exception {
            givenBody(FREE_ROUND_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("1055.00"));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            assertEquals(new BigDecimal("1055.00"), result.getBalance());
            ArgumentCaptor<ResultType> resultTypeCaptor = ArgumentCaptor.forClass(ResultType.class);
            verify(walletService).processBetResult(any(), any(), any(), resultTypeCaptor.capture(), any(), any());
            assertEquals(ResultType.BET_WIN, resultTypeCaptor.getValue());
        }
    }

    // -----------------------------------------------------------------------
    // Validation Errors — errorCode 3 (Scenarios 5.6, 5.7)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Validation Errors — errorCode 3 (Scenarios 5.6, 5.7)")
    class ValidationErrors {

        @Test
        // Scenario 5.6 — blank reqId fails @NotBlank → InvalidRequestException → errorCode 3
        @DisplayName("5.6a: blank reqId → errorCode 3")
        void blankReqId_returns_errorCode3() {
            givenBody(BLANK_REQ_ID_JSON);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(3, result.getErrorCode());
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 5.6 — blank token fails @NotBlank → InvalidRequestException → errorCode 3
        @DisplayName("5.6b: blank token → errorCode 3")
        void blankToken_returns_errorCode3() {
            givenBody(BLANK_TOKEN_JSON);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(3, result.getErrorCode());
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 5.7 — game in BetDto (110) doesn't match GameSession.vendorGameCode → errorCode 3
        @DisplayName("5.7a: game code mismatch between request and session → errorCode 3")
        void gameCodeMismatch_returns_errorCode3() throws Exception {
            givenBody(WIN_JSON); // game=110
            GameSession gs = session("999", "THB"); // session has different game code
            when(gameSessionService.verifyToken("tok-abc")).thenReturn(gs);
            when(requestIdempotentLogService.checkExists(any(BetResultData.class), eq("player001"))).thenReturn(null);
            when(vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(eq("110"), any()))
                    .thenReturn(gs);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(3, result.getErrorCode());
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 5.7 — currency in BetDto (THB) doesn't match GameSession.vendorCurrencyCode → errorCode 3
        @DisplayName("5.7b: currency mismatch between request and session → errorCode 3")
        void currencyMismatch_returns_errorCode3() throws Exception {
            givenBody(WIN_JSON); // currency=THB
            GameSession gs = session("110", "USD"); // session has different currency
            when(gameSessionService.verifyToken("tok-abc")).thenReturn(gs);
            when(requestIdempotentLogService.checkExists(any(BetResultData.class), eq("player001"))).thenReturn(null);
            when(vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(eq("110"), any()))
                    .thenReturn(gs);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(3, result.getErrorCode());
            verifyNoInteractions(walletService);
        }

        @Test
        // No scenario ID — covers JsonProcessingException branch from malformed request body → errorCode 3
        @DisplayName("malformed JSON body → errorCode 3")
        void malformedJson_returns_errorCode3() {
            givenBody(MALFORMED_JSON);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(3, result.getErrorCode());
            verifyNoInteractions(walletService);
        }
    }

    // -----------------------------------------------------------------------
    // Token / Session Errors (Scenarios 8.1, 8.2, 8.3)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Token / Session Errors (Scenarios 8.1, 8.2, 8.3)")
    class SessionErrors {

        @Test
        // Scenario 8.1 — verifyToken throws AuthenticationException → errorCode 4
        @DisplayName("8.1: invalid or expired token → errorCode 4")
        void expiredToken_returns_errorCode4() throws Exception {
            givenBody(WIN_JSON);
            when(gameSessionService.verifyToken("tok-abc"))
                    .thenThrow(new AuthenticationException());

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(4, result.getErrorCode());
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 8.2 — validateEligibleBet throws DisabledAgentPlayerException → errorCode 5
        @DisplayName("8.2: disabled player account → errorCode 5")
        void disabledPlayer_returns_errorCode5() throws Exception {
            givenBody(WIN_JSON);
            GameSession gs = session("110", "THB");
            when(gameSessionService.verifyToken("tok-abc")).thenReturn(gs);
            when(requestIdempotentLogService.checkExists(any(BetResultData.class), eq("player001"))).thenReturn(null);
            when(vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(eq("110"), any()))
                    .thenReturn(gs);
            doThrow(new DisabledAgentPlayerException())
                    .when(validationService).validateEligibleBet(any(), any());

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(5, result.getErrorCode());
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 8.3 — validateEligibleBet throws DisabledVendorLineException → errorCode 5
        @DisplayName("8.3: disabled vendor line → errorCode 5")
        void disabledVendorLine_returns_errorCode5() throws Exception {
            givenBody(WIN_JSON);
            GameSession gs = session("110", "THB");
            when(gameSessionService.verifyToken("tok-abc")).thenReturn(gs);
            when(requestIdempotentLogService.checkExists(any(BetResultData.class), eq("player001"))).thenReturn(null);
            when(vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(eq("110"), any()))
                    .thenReturn(gs);
            doThrow(new DisabledVendorLineException())
                    .when(validationService).validateEligibleBet(any(), any());

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(5, result.getErrorCode());
            verifyNoInteractions(walletService);
        }
    }

    // -----------------------------------------------------------------------
    // Balance Errors — errorCode 2
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Balance Errors — errorCode 2")
    class BalanceErrors {

        @Test
        // No scenario ID — InsufficientBalanceException from walletService → errorCode 2
        @DisplayName("insufficient balance → errorCode 2")
        void insufficientBalance_returns_errorCode2() throws Exception {
            givenBody(WIN_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new InsufficientBalanceException());

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(2, result.getErrorCode());
        }

        @Test
        // No scenario ID — InvalidOperatorResponseException with operatorStatus=11 → errorCode 2
        @DisplayName("operator reports SC_INSUFFICIENT_FUNDS (status=11) → errorCode 2")
        void operatorInsufficientFunds_returns_errorCode2() throws Exception {
            givenBody(WIN_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new InvalidOperatorResponseException(11));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(2, result.getErrorCode());
        }

        @Test
        // No scenario ID — InvalidOperatorResponseException with any other status → errorCode 5
        @DisplayName("operator reports non-11 error status → errorCode 5")
        void operatorOtherError_returns_errorCode5() throws Exception {
            givenBody(WIN_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new InvalidOperatorResponseException(99));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(5, result.getErrorCode());
        }
    }

    // -----------------------------------------------------------------------
    // Idempotency (Scenarios 6.1, 6.2)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Idempotency (Scenarios 6.1, 6.2)")
    class Idempotency {

        @Test
        // Scenario 6.1 — checkExists returns non-null → TransactionStillProcessingException → errorCode 5
        @DisplayName("6.1: concurrent duplicate reqId while first is in-flight → errorCode 5")
        void concurrentDuplicate_returns_errorCode5() throws Exception {
            givenBody(WIN_JSON);
            when(gameSessionService.verifyToken("tok-abc")).thenReturn(session("110", "THB"));
            when(requestIdempotentLogService.checkExists(any(BetResultData.class), eq("player001")))
                    .thenReturn(mock(RequestIdempotentLog.class)); // non-null triggers TransactionStillProcessingException

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(5, result.getErrorCode());
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 6.2 — walletService throws BetResultIdempotentViolationException for a replay.
        // Current behaviour: errorCode 0 (BetVo defaults to SUCCESS) with the stored balance.
        // Pending G4: should return errorCode 1 (ALREADY_ACCEPTED) per Jili spec §7.
        @DisplayName("6.2: replay of previously accepted request → stored balance returned, errorCode 0 (pending G4 → 1)")
        void replayAccepted_returns_storedBalance_errorCode0() throws Exception {
            givenBody(WIN_JSON);
            givenValidSession();
            WalletTransaction wt = mock(WalletTransaction.class);
            when(wt.getBalance()).thenReturn(new BigDecimal("500.00"));
            BetResultIdempotentViolationException ex =
                    new BetResultIdempotentViolationException(wt);
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenThrow(ex);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode()); // pending G4: this should become 1
            assertEquals("player001", result.getUsername());
            assertEquals("THB", result.getCurrency());
            assertEquals(new BigDecimal("500.00"), result.getBalance());
        }

        @Test
        // No scenario ID — covers the generic Exception catch-all in BetAction → errorCode 5
        @DisplayName("unexpected runtime exception → errorCode 5")
        void unexpectedException_returns_errorCode5() throws Exception {
            givenBody(WIN_JSON);
            givenValidSession();
            when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("unexpected"));

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(5, result.getErrorCode());
        }
    }

    // -----------------------------------------------------------------------
    // Free Spin Fork (Scenario 1.1)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Free Spin Fork (Scenarios 1.1, 1.2)")
    class FreeSpin {

        @Test
        // Scenario 1.1 — freeSpinData present routes to JiliFreeSpinPayoutHandler; walletService not called
        @DisplayName("1.1: freeSpinData present routes to handler; walletService never called")
        void freeSpinData_routesToHandler_notToWalletService() throws Exception {
            givenBody(FREE_SPIN_JSON);
            givenValidSession();

            BetVo handlerResult = new BetVo();
            handlerResult.setUsername("player001");
            handlerResult.setCurrency("THB");
            handlerResult.setBalance(new BigDecimal("1055.00"));
            handlerResult.setToken("tok-abc");
            when(jiliFreeSpinPayoutProcessor.process(any(BetDto.class), any(), any(), any()))
                    .thenReturn(handlerResult);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            assertEquals("player001", result.getUsername());
            assertEquals("THB", result.getCurrency());
            assertEquals(new BigDecimal("1055.00"), result.getBalance());
            assertEquals("tok-abc", result.getToken());
            verify(jiliFreeSpinPayoutProcessor).process(any(BetDto.class), eq("player001"), eq("THB"), eq("tok-abc"));
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 1.2 — zero winloseAmount still routes to processor; balance unchanged; walletService not called
        @DisplayName("1.2: free spin with zero win routes to handler; balance unchanged")
        void freeSpinData_zeroWin_routesToHandler_balanceUnchanged() throws Exception {
            givenBody(FREE_SPIN_ZERO_WIN_JSON);
            givenValidSession();

            BetVo handlerResult = new BetVo();
            handlerResult.setUsername("player001");
            handlerResult.setCurrency("THB");
            handlerResult.setBalance(new BigDecimal("1000.00")); // prior balance, no credit
            handlerResult.setToken("tok-abc");
            when(jiliFreeSpinPayoutProcessor.process(any(BetDto.class), any(), any(), any()))
                    .thenReturn(handlerResult);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            assertEquals(new BigDecimal("1000.00"), result.getBalance());
            verify(jiliFreeSpinPayoutProcessor).process(any(BetDto.class), eq("player001"), eq("THB"), eq("tok-abc"));
            verifyNoInteractions(walletService);
        }

        @Test
        // Scenario 1.3 — remain=0 (last round); same code path as 1.1; no guard rejects a zero-remain value
        @DisplayName("1.3: last free spin round (remain=0) still routes to handler; errorCode 0")
        void freeSpinData_lastRound_remain0_routesToHandler() throws Exception {
            givenBody(FREE_SPIN_LAST_ROUND_JSON);
            givenValidSession();

            BetVo handlerResult = new BetVo();
            handlerResult.setUsername("player001");
            handlerResult.setCurrency("THB");
            handlerResult.setBalance(new BigDecimal("1030.00"));
            handlerResult.setToken("tok-abc");
            when(jiliFreeSpinPayoutProcessor.process(any(BetDto.class), any(), any(), any()))
                    .thenReturn(handlerResult);

            BetVo result = betAction.betRequest(httpServletRequest);

            assertEquals(0, result.getErrorCode());
            assertEquals("player001", result.getUsername());
            assertEquals("THB", result.getCurrency());
            assertEquals(new BigDecimal("1030.00"), result.getBalance());
            assertEquals("tok-abc", result.getToken());
            verify(jiliFreeSpinPayoutProcessor).process(any(BetDto.class), eq("player001"), eq("THB"), eq("tok-abc"));
            verifyNoInteractions(walletService);
        }
    }
}
