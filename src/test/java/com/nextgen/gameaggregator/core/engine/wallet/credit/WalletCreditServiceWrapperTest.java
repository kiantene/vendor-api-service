package com.nextgen.gameaggregator.core.engine.wallet.credit;

import org.mockito.ArgumentCaptor;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.core.engine.wallet.WalletRequestLog;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.validator.VendorRequestValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Locks the framework guard contract for the credit pipeline. The wrapper must run guards
 * in this order and dispatch on {@link CreditContext#isRefund()}:
 * <ol>
 *   <li>{@code walletCreditValidator.validateRequestContext}</li>
 *   <li>{@code creditProcessor.tryReplay} — key-only replay peek. Returns early on a recorded
 *       SUCCESS, <b>before any session work</b>, so a replay cannot fail on state that changed
 *       after it succeeded and cannot mint a throwaway session token.</li>
 *   <li>Settle: {@code gameSessionDataService.getGameSession} (strict).<br>
 *       Refund: {@code gameSessionDataService.getOrCreate} (session-regen-on-expiry).</li>
 *   <li>{@code creditContextEnricher.enrichByGameSession}</li>
 *   <li>{@code creditProcessor.process}</li>
 * </ol>
 * The dependency justification for each step lives on {@link WalletCreditServiceWrapper} itself.
 */
@ExtendWith(MockitoExtension.class)
class WalletCreditServiceWrapperTest {

    @Mock
    private CreditProcessor creditProcessor;
    @Mock
    private GameSessionDataService gameSessionDataService;
    @Mock
    private CreditContextEnricher creditContextEnricher;
    @Mock
    private WalletCreditValidator walletCreditValidator;
    @Mock
    private VendorRequestValidator vendorRequestValidator;

    @Mock
    private DebitReferenceGuard debitReferenceGuard;

    private WalletCreditServiceWrapper wrapper;

    @BeforeEach
    void setUp() {
        // A real resolver over the mocked session service and guard: the session-mode dispatch these
        // tests pin now lives in CreditSessionResolver. The mocked guard's load() answers an empty
        // list by default, i.e. a credit naming no debit, which keeps the session path.
        wrapper = new WalletCreditServiceWrapper(
                creditProcessor,
                new CreditSessionResolver(gameSessionDataService, debitReferenceGuard),
                creditContextEnricher,
                walletCreditValidator,
                vendorRequestValidator);
    }

    @AfterEach
    void clearHolder() {
        CreditContextHolder.clear();
    }

    private CreditContext settleContext() {
        return CreditContext.builder()
                .traceId("trace-2")
                .idempotencyKey("transfer-2")
                .vendorTxnId("transfer-2")
                .vendorRoundId("round-1")
                .vendorAmount(new BigDecimal("8.00"))
                .vendorBetAmount(new BigDecimal("5.00"))
                .vendorWinAmount(new BigDecimal("8.00"))
                .vendorWinLoss(new BigDecimal("3.00"))
                .vendorTimestamp(1700000060000L)
                .vendorPlayerUsername("alice")
                .vendorCurrency("PHP")
                .vendorGameCode("12")
                .isRefund(false)
                .isRoundEnded(Boolean.TRUE)
                .build();
    }

    private CreditContext refundContext() {
        return CreditContext.builder()
                .traceId("trace-3")
                .idempotencyKey("transfer-3")
                .vendorTxnId("transfer-3")
                .vendorRoundId("round-1")
                .vendorAmount(new BigDecimal("5.00"))
                .vendorTimestamp(1700000120000L)
                .vendorPlayerUsername("alice")
                .vendorCurrency("PHP")
                .vendorGameCode("12")
                .isRefund(true)
                .referencedVendorDebitTxnIds(List.of("100"))
                .build();
    }

    private PlayerBalanceData sampleBalance() {
        return new PlayerBalanceData("alice", "PHP", new BigDecimal("103.00"),
                System.currentTimeMillis());
    }

    @Test
    void process_settle_happy_path_uses_strict_session_lookup() {
        CreditContext ctx = settleContext();
        GameSession session = new GameSession();
        PlayerBalanceData balance = sampleBalance();

        when(gameSessionDataService.getGameSession(ctx)).thenReturn(session);
        when(creditProcessor.process(any())).thenReturn(balance);

        PlayerBalanceData result = wrapper.initialise(ctx).process();

        assertEquals(balance, result);

        InOrder ordered = inOrder(walletCreditValidator, gameSessionDataService,
                creditContextEnricher, vendorRequestValidator, creditProcessor);
        ordered.verify(walletCreditValidator).validateRequestContext(ctx);
        ordered.verify(creditProcessor).tryReplay(any(CreditWrapperContext.class));
        ordered.verify(gameSessionDataService).getGameSession(ctx);
        ordered.verify(creditContextEnricher).enrichByGameSession(ctx, session);
        // After enrichment: the check compares request fields the enricher backfills from the session.
        ordered.verify(vendorRequestValidator).validateVendorRequestWithGameSession(session, ctx);
        ordered.verify(creditProcessor).process(any(CreditWrapperContext.class));

        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());
    }

    @Test
    void process_refund_happy_path_uses_getOrCreate_for_session_regen() {
        // Refund must succeed even with an aged-out player session — wrapper picks
        // getOrCreate which regenerates the session when vendorPlayerUsername is present.
        CreditContext ctx = refundContext();
        GameSession session = new GameSession();
        PlayerBalanceData balance = sampleBalance();

        when(gameSessionDataService.getOrCreate(eq(ctx), anyBoolean())).thenReturn(session);
        when(creditProcessor.process(any())).thenReturn(balance);

        PlayerBalanceData result = wrapper.initialise(ctx).process();

        assertEquals(balance, result);

        InOrder ordered = inOrder(walletCreditValidator, gameSessionDataService,
                creditContextEnricher, vendorRequestValidator, creditProcessor);
        ordered.verify(walletCreditValidator).validateRequestContext(ctx);
        ordered.verify(creditProcessor).tryReplay(any(CreditWrapperContext.class));
        ordered.verify(gameSessionDataService).getOrCreate(eq(ctx), anyBoolean());
        ordered.verify(creditContextEnricher).enrichByGameSession(ctx, session);
        // After enrichment: the check compares request fields the enricher backfills from the session.
        ordered.verify(vendorRequestValidator).validateVendorRequestWithGameSession(session, ctx);
        ordered.verify(creditProcessor).process(any(CreditWrapperContext.class));

        verify(gameSessionDataService, never()).getGameSession(any());
    }

    @Test
    void process_refund_always_mints_a_token_on_regeneration() {
        // An aged-out refund has no usable token and the operator bet_credit call requires one, for
        // every vendor. eq(true) — not anyBoolean() — is what locks this; anyBoolean() would still
        // pass if the wrapper went back to forwarding the request's null token.
        CreditContext ctx = refundContext();
        when(gameSessionDataService.getOrCreate(eq(ctx), eq(true))).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        verify(gameSessionDataService).getOrCreate(eq(ctx), eq(true));
        verify(gameSessionDataService, never()).getOrCreate(eq(ctx), eq(false));
    }

    @Test
    void process_settle_never_uses_getOrCreate() {
        // Regeneration (and its token mint) is a REFUND-only carveout: settle must stay on the
        // strict lookup so an expired session still fails closed.
        CreditContext ctx = settleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        verify(gameSessionDataService).getGameSession(eq(ctx));
        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());
    }

    @Test
    void process_settle_session_expired_throws_and_skips_downstream() {
        CreditContext ctx = settleContext();
        doNothing().when(walletCreditValidator).validateRequestContext(ctx);
        when(gameSessionDataService.getGameSession(ctx))
                .thenThrow(new GameSessionExpiredException());

        assertThrows(GameSessionExpiredException.class,
                () -> wrapper.initialise(ctx).process());

        verify(creditContextEnricher, never()).enrichByGameSession(any(), any());
        verify(creditProcessor, never()).process(any());
    }

    @Test
    void process_refund_session_irrecoverable_throws_and_skips_downstream() {
        // getOrCreate still throws when even username can't recover the session
        // (e.g. vendorPlayer not found). Refund cannot proceed without a session.
        CreditContext ctx = refundContext();
        when(gameSessionDataService.getOrCreate(eq(ctx), anyBoolean()))
                .thenThrow(new GameSessionExpiredException());

        assertThrows(GameSessionExpiredException.class,
                () -> wrapper.initialise(ctx).process());

        verify(creditContextEnricher, never()).enrichByGameSession(any(), any());
        verify(creditProcessor, never()).process(any());
    }

    @Test
    void process_settle_runs_no_eligibility_check() {
        // OVI-2156: a settle must complete after the player's token has been terminated, so the
        // credit path has no session-derived eligibility check. The only validator call is the
        // request-shape check.
        CreditContext ctx = settleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        verify(walletCreditValidator).validateRequestContext(ctx);
        verifyNoMoreInteractions(walletCreditValidator);
        verify(creditProcessor).process(any(CreditWrapperContext.class));
    }

    @Test
    void process_request_session_mismatch_skips_processor() {
        // OVI-2166: a request naming a different currency, username or game code than its session
        // must be rejected (core InvalidRequestException -> 8000) before any money moves.
        CreditContext ctx = settleContext();
        GameSession session = new GameSession();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(session);
        doThrow(new InvalidRequestException("Invalid VendorCurrency"))
                .when(vendorRequestValidator).validateVendorRequestWithGameSession(session, ctx);

        assertThrows(InvalidRequestException.class,
                () -> wrapper.initialise(ctx).process());

        verify(creditProcessor, never()).process(any());
    }

    @Test
    void process_request_validation_fail_skips_everything() {
        CreditContext ctx = settleContext();
        doThrow(new InvalidRequestException("bad request"))
                .when(walletCreditValidator).validateRequestContext(ctx);

        assertThrows(InvalidRequestException.class,
                () -> wrapper.initialise(ctx).process());

        verify(creditProcessor, never()).tryReplay(any());
        verify(gameSessionDataService, never()).getGameSession(any());
        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());
        verify(creditContextEnricher, never()).enrichByGameSession(any(), any());
        verify(creditProcessor, never()).process(any());
    }

    @Test
    void process_peek_replay_returns_early_and_skips_session_enrich_and_processor() {
        // The headline fix: an already-completed credit answers from its recorded result without
        // touching the session lookup, the enrichment chain, or the processor.
        CreditContext ctx = settleContext();
        PlayerBalanceData recorded = sampleBalance();
        when(creditProcessor.tryReplay(any(CreditWrapperContext.class))).thenReturn(Optional.of(recorded));

        PlayerBalanceData result = wrapper.initialise(ctx).process();

        assertEquals(recorded, result);
        verify(gameSessionDataService, never()).getGameSession(any());
        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());
        verify(creditContextEnricher, never()).enrichByGameSession(any(), any());
        verify(vendorRequestValidator, never()).validateVendorRequestWithGameSession(any(), any());
        verify(creditProcessor, never()).process(any());
    }

    @Test
    void process_peek_replay_refund_never_mints_session_token() {
        // A JDB Action 11 refund replay must not regenerate the session, and in particular must not
        // mint a fresh UUID token that is thrown away — regeneration is unreachable
        // once the recorded result answers the request.
        CreditContext ctx = refundContext();
        when(creditProcessor.tryReplay(any(CreditWrapperContext.class))).thenReturn(Optional.of(sampleBalance()));

        wrapper.initialise(ctx).process();

        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());
        verify(gameSessionDataService, never()).getGameSession(any());
    }

    @Test
    void process_peek_in_flight_throws_before_session_lookup() {
        // An in-flight collision stops the request at the peek — no session work, no enrichment.
        CreditContext ctx = settleContext();
        when(creditProcessor.tryReplay(any(CreditWrapperContext.class)))
                .thenThrow(new DuplicateRequestException("still being processed"));

        assertThrows(DuplicateRequestException.class,
                () -> wrapper.initialise(ctx).process());

        verify(gameSessionDataService, never()).getGameSession(any());
        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());
        verify(creditContextEnricher, never()).enrichByGameSession(any(), any());
        verify(creditProcessor, never()).process(any());
    }

    @Test
    void process_peek_miss_then_claim_time_replay_still_returns_prior_balance() {
        // The peek is advisory. When it misses — including when a concurrent attempt completes just
        // after it — the full pipeline runs and the authoritative claim inside process() catches the
        // replay instead. Same answer, one pipeline later.
        CreditContext ctx = settleContext();
        GameSession session = new GameSession();
        PlayerBalanceData recorded = sampleBalance();
        when(creditProcessor.tryReplay(any(CreditWrapperContext.class))).thenReturn(Optional.empty());
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(session);
        when(creditProcessor.process(any())).thenReturn(recorded);

        PlayerBalanceData result = wrapper.initialise(ctx).process();

        assertEquals(recorded, result);

        InOrder ordered = inOrder(walletCreditValidator, creditProcessor, gameSessionDataService,
                creditContextEnricher);
        ordered.verify(walletCreditValidator).validateRequestContext(ctx);
        ordered.verify(creditProcessor).tryReplay(any(CreditWrapperContext.class));
        ordered.verify(gameSessionDataService).getGameSession(ctx);
        ordered.verify(creditContextEnricher).enrichByGameSession(ctx, session);
        ordered.verify(creditProcessor).process(any(CreditWrapperContext.class));
    }

    @Test
    void process_clears_holder_when_peek_throws() {
        CreditContext ctx = settleContext();
        when(creditProcessor.tryReplay(any(CreditWrapperContext.class)))
                .thenThrow(new DuplicateRequestException("still being processed"));

        assertThrows(DuplicateRequestException.class,
                () -> wrapper.initialise(ctx).process());

        assertFalse(CreditContextHolder.isInitialized());
    }

    @Test
    void process_clears_holder_on_happy_path_settle() {
        CreditContext ctx = settleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        assertFalse(CreditContextHolder.isInitialized());
    }

    @Test
    void process_clears_holder_on_happy_path_refund() {
        CreditContext ctx = refundContext();
        when(gameSessionDataService.getOrCreate(eq(ctx), anyBoolean())).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        assertFalse(CreditContextHolder.isInitialized());
    }

    @Test
    void process_clears_holder_when_processor_throws() {
        CreditContext ctx = settleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenThrow(new DuplicateRequestException("SENT"));

        assertThrows(DuplicateRequestException.class,
                () -> wrapper.initialise(ctx).process());

        assertFalse(CreditContextHolder.isInitialized());
    }

    @Test
    void process_uses_eq_context_in_session_lookup() {
        CreditContext ctx = settleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        verify(gameSessionDataService).getGameSession(eq(ctx));
    }

    @Test
    void process_failure_recordsTheErrorOnAVendorOwnedLog() {
        // OVI-3176: JDB publishes its own log, and VendorExceptionAspect turns the exception into a
        // response before JDB's catch could record it. The wrapper must record it on the way out.
        CreditContext ctx = settleContext();
        LogContext logContext = new LogContext();
        HttpRequestLog vendorLog = new HttpRequestLog();
        WalletRequestLog.declareVendorOwned(logContext, vendorLog);
        LogContextHolder.set(logContext);
        try {
            when(creditProcessor.process(any())).thenThrow(new InvalidRequestException("Invalid VendorCurrency"));
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());

            assertThrows(InvalidRequestException.class, () -> wrapper.initialise(ctx).process());

            assertNotNull(vendorLog.getErrorMessage(), "a failed request must not publish an empty error");
            assertTrue(vendorLog.getErrorMessage().contains("InvalidRequestException"));
            assertEquals("Invalid VendorCurrency", vendorLog.getExceptionMessage());
        } finally {
            LogContextHolder.clear();
        }
    }

    // ---------- Credit resolved against its debit ----------

    private static KvDoc<DebitTxn> debitWithSnapshot(String player) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(1087);
        agentMeta.setUsername("op-alice");
        agentMeta.setCurrency("PHP");
        agentMeta.setGameCode("JDB_12");
        agentMeta.setSession("ga-token-1");
        DebitTxn debit = DebitTxn.of("jdb", "100", 1L);
        debit.setVendorPlayerUsername(player);
        debit.setVendorCurrency("PHP");
        debit.setVendorGameCode("12");
        debit.setAgentMeta(agentMeta);
        return new KvDoc<>(debit.getId(), 7L, debit);
    }

    @Test
    void process_debitWithSnapshot_readsNoSession_andPinsTheOperatorTargetOnTheDebit() {
        CreditContext ctx = refundContext();
        List<KvDoc<DebitTxn>> loaded = List.of(debitWithSnapshot("alice"));
        when(debitReferenceGuard.load(ctx)).thenReturn(loaded);
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        verify(gameSessionDataService, never()).getGameSession(any());
        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());

        ArgumentCaptor<GameSession> view = ArgumentCaptor.forClass(GameSession.class);
        InOrder ordered = inOrder(creditContextEnricher, vendorRequestValidator, creditProcessor);
        ordered.verify(creditContextEnricher).enrichByGameSession(eq(ctx), view.capture());
        ordered.verify(creditContextEnricher).applyDebitSnapshot(ctx, loaded.get(0).getPayload().getAgentMeta());
        // The request is checked against the debit exactly as it would be against a session.
        ordered.verify(vendorRequestValidator).validateVendorRequestWithGameSession(view.getValue(), ctx);
        ArgumentCaptor<CreditWrapperContext> state = ArgumentCaptor.forClass(CreditWrapperContext.class);
        ordered.verify(creditProcessor).process(state.capture());

        assertEquals("ga-token-1", view.getValue().getToken());
        assertEquals(loaded, state.getValue().getReferencedDebits(),
                "the policy judges the debits loaded here, not a second read");
    }

    @Test
    void process_uidNotOwningTheDebit_failsBeforeEnrichmentOrTheOperator() {
        CreditContext ctx = refundContext();
        when(debitReferenceGuard.load(ctx)).thenReturn(List.of(debitWithSnapshot("tm63ux7pc")));

        assertThrows(GameSessionExpiredException.class, () -> wrapper.initialise(ctx).process());

        verify(creditContextEnricher, never()).enrichByGameSession(any(), any());
        verify(creditProcessor, never()).process(any());
    }

    @Test
    void process_sessionPath_neverAppliesADebitSnapshot() {
        CreditContext ctx = settleContext();
        when(gameSessionDataService.getGameSession(ctx)).thenReturn(new GameSession());
        when(creditProcessor.process(any())).thenReturn(sampleBalance());

        wrapper.initialise(ctx).process();

        verify(creditContextEnricher, never()).applyDebitSnapshot(any(), any());
    }
}
