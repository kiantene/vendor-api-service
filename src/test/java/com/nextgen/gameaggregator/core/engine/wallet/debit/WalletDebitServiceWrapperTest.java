package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.core.engine.wallet.WalletRequestLog;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.validator.VendorRequestValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Locks the framework guard contract for the debit pipeline. The wrapper must run guards
 * in this exact order on every call (matches BET path's {@code WalletBetServiceWrapper}):
 * <ol>
 *   <li>{@code walletDebitValidator.validateRequestContext}</li>
 *   <li>{@code debitProcessor.tryReplay} — key-only replay peek. Returns early on a recorded
 *       SUCCESS, <b>before any session work</b>, so a replay cannot fail on state that changed
 *       after it succeeded (notably a session that has since aged out).</li>
 *   <li>{@code gameSessionDataService.getGameSession}</li>
 *   <li>{@code debitContextEnricher.enrichByGameSession}</li>
 *   <li>{@code walletDebitValidator.validateBusinessState}</li>
 *   <li>{@code debitProcessor.process}</li>
 * </ol>
 * <p>
 * A failure at any guard short-circuits and the processor is never called. The
 * {@code DebitContextHolder} is always cleaned up — exception or not.
 */
@ExtendWith(MockitoExtension.class)
class WalletDebitServiceWrapperTest {

    @Mock
    private DebitProcessor debitProcessor;
    @Mock
    private GameSessionDataService gameSessionDataService;
    @Mock
    private DebitContextEnricher debitContextEnricher;
    @Mock
    private WalletDebitValidator walletDebitValidator;
    @Mock
    private VendorRequestValidator vendorRequestValidator;

    @InjectMocks
    private WalletDebitServiceWrapper wrapper;

    @AfterEach
    void clearHolder() {
        // Defensive: wrapper.process()'s finally clears the holder, but exception paths
        // before initialise() might not. Keep ThreadLocal clean between tests.
        DebitContextHolder.clear();
    }

    private DebitContext sampleContext() {
        return DebitContext.builder()
                .traceId("trace-1")
                .idempotencyKey("transfer-1")
                .vendorTxnId("transfer-1")
                .vendorRoundId("round-1")
                .vendorAmount(new BigDecimal("5.00"))
                .vendorTimestamp(1700000000000L)
                .vendorPlayerUsername("alice")
                .vendorCurrency("PHP")
                .vendorGameCode("12")
                .build();
    }

    private PlayerBalanceData sampleBalance() {
        return new PlayerBalanceData("alice", "PHP", new BigDecimal("95.00"),
                System.currentTimeMillis());
    }

    @Test
    void process_happy_path_runs_guards_in_order_then_processor() {
        DebitContext ctx = sampleContext();
        GameSession session = new GameSession();
        PlayerBalanceData balance = sampleBalance();

        when(gameSessionDataService.getGameSession(ctx)).thenReturn(session);
        when(debitProcessor.process(any())).thenReturn(balance);

        PlayerBalanceData result = wrapper.initialise(ctx).process();

        assertEquals(balance, result);

        InOrder ordered = inOrder(walletDebitValidator, gameSessionDataService,
                debitContextEnricher, vendorRequestValidator, debitProcessor);
        ordered.verify(walletDebitValidator).validateRequestContext(ctx);
        ordered.verify(debitProcessor).tryReplay(any(DebitWrapperContext.class));
        ordered.verify(gameSessionDataService).getGameSession(ctx);
        ordered.verify(debitContextEnricher).enrichByGameSession(ctx, session);
        // After enrichment: the check compares request fields the enricher backfills from the session.
        ordered.verify(vendorRequestValidator).validateVendorRequestWithGameSession(session, ctx);
        ordered.verify(walletDebitValidator).validateBusinessState(session, ctx);
        ordered.verify(debitProcessor).process(any(DebitWrapperContext.class));
    }

    @Test
    void process_clears_DebitContextHolder_on_happy_path() {
        DebitContext ctx = sampleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(debitProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        assertFalse(DebitContextHolder.isInitialized(),
                "Holder must be cleared after process() returns");
    }

    @Test
    void process_request_validation_fail_skips_session_and_processor() {
        DebitContext ctx = sampleContext();
        doThrow(new InvalidRequestException("bad request"))
                .when(walletDebitValidator).validateRequestContext(ctx);

        assertThrows(InvalidRequestException.class,
                () -> wrapper.initialise(ctx).process());

        verify(debitProcessor, never()).tryReplay(any());
        verify(gameSessionDataService, never()).getGameSession(any());
        verify(debitContextEnricher, never()).enrichByGameSession(any(), any());
        verify(walletDebitValidator, never()).validateBusinessState(any(), any());
        verify(debitProcessor, never()).process(any());
    }

    @Test
    void process_peek_replay_returns_early_and_skips_session_enrich_and_processor() {
        // An already-completed debit answers from its recorded result. Without this, a replay
        // arriving after the session has aged out gets GameSessionExpiredException -> JDB 7501
        // for a debit GA has already made.
        DebitContext ctx = sampleContext();
        PlayerBalanceData recorded = sampleBalance();
        when(debitProcessor.tryReplay(any(DebitWrapperContext.class))).thenReturn(Optional.of(recorded));

        PlayerBalanceData result = wrapper.initialise(ctx).process();

        assertEquals(recorded, result);
        verify(gameSessionDataService, never()).getGameSession(any());
        verify(debitContextEnricher, never()).enrichByGameSession(any(), any());
        verify(vendorRequestValidator, never()).validateVendorRequestWithGameSession(any(), any());
        verify(walletDebitValidator, never()).validateBusinessState(any(), any());
        verify(debitProcessor, never()).process(any());
    }

    @Test
    void process_peek_in_flight_throws_before_session_lookup() {
        DebitContext ctx = sampleContext();
        when(debitProcessor.tryReplay(any(DebitWrapperContext.class)))
                .thenThrow(new DuplicateRequestException("still being processed"));

        assertThrows(DuplicateRequestException.class,
                () -> wrapper.initialise(ctx).process());

        verify(gameSessionDataService, never()).getGameSession(any());
        verify(debitContextEnricher, never()).enrichByGameSession(any(), any());
        verify(debitProcessor, never()).process(any());
    }

    @Test
    void process_peek_miss_then_claim_time_replay_still_returns_prior_balance() {
        // The peek is advisory. When it misses, the full pipeline runs and the authoritative claim
        // inside process() catches the replay instead. Same answer, one pipeline later.
        DebitContext ctx = sampleContext();
        GameSession session = new GameSession();
        PlayerBalanceData recorded = sampleBalance();
        when(debitProcessor.tryReplay(any(DebitWrapperContext.class))).thenReturn(Optional.empty());
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(session);
        when(debitProcessor.process(any())).thenReturn(recorded);

        PlayerBalanceData result = wrapper.initialise(ctx).process();

        assertEquals(recorded, result);

        InOrder ordered = inOrder(walletDebitValidator, debitProcessor, gameSessionDataService,
                debitContextEnricher);
        ordered.verify(walletDebitValidator).validateRequestContext(ctx);
        ordered.verify(debitProcessor).tryReplay(any(DebitWrapperContext.class));
        ordered.verify(gameSessionDataService).getGameSession(ctx);
        ordered.verify(debitContextEnricher).enrichByGameSession(ctx, session);
        ordered.verify(walletDebitValidator).validateBusinessState(session, ctx);
        ordered.verify(debitProcessor).process(any(DebitWrapperContext.class));
    }

    @Test
    void process_clears_holder_when_peek_throws() {
        DebitContext ctx = sampleContext();
        when(debitProcessor.tryReplay(any(DebitWrapperContext.class)))
                .thenThrow(new DuplicateRequestException("still being processed"));

        assertThrows(DuplicateRequestException.class,
                () -> wrapper.initialise(ctx).process());

        assertFalse(DebitContextHolder.isInitialized(),
                "Holder must be cleared even when the replay peek throws");
    }

    @Test
    void process_clears_holder_when_request_validation_fails() {
        DebitContext ctx = sampleContext();
        doThrow(new InvalidRequestException("bad request"))
                .when(walletDebitValidator).validateRequestContext(ctx);

        assertThrows(InvalidRequestException.class,
                () -> wrapper.initialise(ctx).process());

        assertFalse(DebitContextHolder.isInitialized(),
                "Holder must be cleared even when a guard throws");
    }

    @Test
    void process_session_expired_skips_enrich_and_processor() {
        DebitContext ctx = sampleContext();
        doNothing().when(walletDebitValidator).validateRequestContext(ctx);
        when(gameSessionDataService.getGameSession(ctx))
                .thenThrow(new GameSessionExpiredException());

        assertThrows(GameSessionExpiredException.class,
                () -> wrapper.initialise(ctx).process());

        verify(debitContextEnricher, never()).enrichByGameSession(any(), any());
        verify(walletDebitValidator, never()).validateBusinessState(any(), any());
        verify(debitProcessor, never()).process(any());
    }

    @Test
    void process_request_session_mismatch_skips_business_state_and_processor() {
        // OVI-2166: a request naming a different currency, username or game code than its session
        // must be rejected (core InvalidRequestException -> 8000) before any money moves.
        DebitContext ctx = sampleContext();
        GameSession session = new GameSession();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(session);
        doThrow(new InvalidRequestException("Invalid VendorCurrency"))
                .when(vendorRequestValidator).validateVendorRequestWithGameSession(session, ctx);

        assertThrows(InvalidRequestException.class,
                () -> wrapper.initialise(ctx).process());

        verify(walletDebitValidator, never()).validateBusinessState(any(), any());
        verify(debitProcessor, never()).process(any());
    }

    @Test
    void process_business_state_failure_skips_processor() {
        DebitContext ctx = sampleContext();
        GameSession session = new GameSession();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(session);
        doThrow(new BetNotAllowedException())
                .when(walletDebitValidator).validateBusinessState(session, ctx);

        assertThrows(BetNotAllowedException.class,
                () -> wrapper.initialise(ctx).process());

        // Enricher DOES run before validateBusinessState — confirm ordering.
        verify(debitContextEnricher).enrichByGameSession(ctx, session);
        verify(debitProcessor, never()).process(any());
    }

    @Test
    void process_propagates_processor_DuplicateRequestException_and_clears_holder() {
        DebitContext ctx = sampleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(debitProcessor.process(any())).thenThrow(new DuplicateRequestException("SENT"));

        assertThrows(DuplicateRequestException.class,
                () -> wrapper.initialise(ctx).process());

        assertFalse(DebitContextHolder.isInitialized(),
                "Holder cleared even when processor throws (idempotency replay path)");
    }

    @Test
    void process_uses_strict_session_lookup_not_getOrCreate() {
        DebitContext ctx = sampleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(debitProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        verify(gameSessionDataService).getGameSession(eq(ctx));
        verify(gameSessionDataService, never()).getOrCreate(any());
    }

    @Test
    void process_failure_recordsTheErrorOnAVendorOwnedLog() {
        // OVI-3176: JDB publishes its own log, and VendorExceptionAspect turns the exception into a
        // response before JDB's catch could record it. The wrapper must record it on the way out.
        DebitContext ctx = sampleContext();
        LogContext logContext = new LogContext();
        HttpRequestLog vendorLog = new HttpRequestLog();
        WalletRequestLog.declareVendorOwned(logContext, vendorLog);
        LogContextHolder.set(logContext);
        try {
            when(debitProcessor.process(any())).thenThrow(new InvalidRequestException("Invalid VendorCurrency"));
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());

            assertThrows(InvalidRequestException.class, () -> wrapper.initialise(ctx).process());

            assertNotNull(vendorLog.getErrorMessage(), "a failed request must not publish an empty error");
            assertTrue(vendorLog.getErrorMessage().contains("InvalidRequestException"));
            assertEquals("Invalid VendorCurrency", vendorLog.getExceptionMessage());
        } finally {
            LogContextHolder.clear();
        }
    }
}
