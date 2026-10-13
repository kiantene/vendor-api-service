package com.nextgen.gameaggregator.core.engine.wallet.credit;

import java.util.List;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyEndedException;
import com.nextgen.gameaggregator.core.exception.BetAlreadySettledException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.enums.GameRoundState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orchestration-level coverage for {@link CreditProcessor}. Inner-phase behavior
 * (gates, idempotency switch, operator call, audit publish) is covered by the
 * per-collaborator test classes.
 *
 * <p>Locks the processor half of the ordering contract:
 * {@code policy → claim → operator → audit}, with {@link CreditProcessor#tryReplay} as an
 * optional fast path the wrapper calls first, and a re-peek on policy rejection so
 * {@link CreditProcessor#process} stays correct even when {@code tryReplay} was skipped.
 */
@ExtendWith(MockitoExtension.class)
class CreditProcessorTest {

    @Mock private CreditPolicy creditPolicy;
    @Mock private CreditIdempotencyResolver idempotencyResolver;
    @Mock private CreditOperatorInvoker operatorInvoker;
    @Mock private CreditAuditPublisher auditPublisher;

    @InjectMocks private CreditProcessor processor;

    private CreditContext.CreditContextBuilder<?, ?> baseBuilder() {
        return CreditContext.builder()
                .traceId("trace-c")
                .idempotencyKey("transfer-200")
                .vendorTxnId("transfer-200")
                .vendorRoundId("round-1")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorCurrency("PHP")
                .currencyCode("PHP")
                .currencyId(7)
                .vendorId(88)
                .agentId(1)
                .agentPlayerUsername("op-bob")
                .vendorGameCode("SG12Zodiacs")
                .gameCode("SG12Zodiacs")
                .token("session-token")
                .fromVendorRate(BigDecimal.ONE)
                .toVendorRate(BigDecimal.ONE);
    }

    private CreditContext settleContext() {
        return baseBuilder()
                .isRefund(false)
                .vendorAmount(new BigDecimal("12.00"))
                .vendorBetAmount(new BigDecimal("5.50"))
                .vendorWinAmount(new BigDecimal("12.00"))
                .vendorBetTime(1700000000000L)
                .vendorSettleTime(1700000060000L)
                .isRoundEnded(true)
                .build();
    }

    private CreditTxn freshTxn() {
        CreditTxn txn = CreditTxn.of("jdb", "transfer-200", GameRoundState.SETTLED, 1L);
        txn.setVendorPlayerUsername("bob");
        txn.setVendorRoundId("round-1");
        return txn;
    }

    @Test
    void happyPath_invokesOperatorAndAuditPublisher() {
        CreditTxn txn = freshTxn();
        when(idempotencyResolver.resolve(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.proceed(txn));
        when(operatorInvoker.invoke(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.any(CreditTxn.class),
                ArgumentMatchers.anyLong()))
                .thenReturn(new PlayerBalanceData("bob", "PHP", new BigDecimal("106.50"), 0L));

        PlayerBalanceData result = processor.process(new CreditWrapperContext(settleContext()));

        assertNotNull(result);
        assertEquals(0, new BigDecimal("106.50").compareTo(result.getBalance()));

        verify(creditPolicy).evaluate(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditConfig.class), ArgumentMatchers.any());
        verify(auditPublisher).publish(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.eq(txn),
                ArgumentMatchers.any(CreditConfig.class),
                ArgumentMatchers.argThat(b -> b != null && b.compareTo(new BigDecimal("106.50")) == 0),
                ArgumentMatchers.eq(true));
    }

    @Test
    void orderingContract_policyThenClaimThenOperatorThenAudit() {
        CreditTxn txn = freshTxn();
        when(idempotencyResolver.resolve(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.proceed(txn));
        when(operatorInvoker.invoke(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.any(CreditTxn.class),
                ArgumentMatchers.anyLong()))
                .thenReturn(new PlayerBalanceData("bob", "PHP", new BigDecimal("106.50"), 0L));

        processor.process(new CreditWrapperContext(settleContext()));

        InOrder ordered = Mockito.inOrder(creditPolicy, idempotencyResolver, operatorInvoker, auditPublisher);
        ordered.verify(creditPolicy).evaluate(
                ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditConfig.class),
                ArgumentMatchers.any());
        ordered.verify(idempotencyResolver).resolve(
                ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong());
        ordered.verify(operatorInvoker).invoke(
                ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditTxn.class),
                ArgumentMatchers.anyLong());
        ordered.verify(auditPublisher).publish(
                ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.eq(txn),
                ArgumentMatchers.any(CreditConfig.class),
                ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void replayWithReturnSuccessOnDuplicate_returnsPriorBalanceAndSkipsSideEffects() {
        // Claim-time replay: the wrapper's peek missed (or was skipped) and resolve() caught it.
        when(idempotencyResolver.resolve(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("106.50")));

        CreditWrapperContext state = new CreditWrapperContext(settleContext());
        state.getConfig().returnSuccessOnDuplicate(true);

        PlayerBalanceData result = processor.process(state);

        assertEquals(0, new BigDecimal("106.50").compareTo(result.getBalance()));
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publish(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void replayWithoutReturnSuccessOnDuplicate_throwsDuplicateRequestAndSkipsSideEffects() {
        when(idempotencyResolver.resolve(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("106.50")));

        // Default config.returnSuccessOnDuplicate == false — vendor wants the wire-level
        // DUPLICATE_TRANSACTION code, not a silent dedupe.
        CreditWrapperContext state = new CreditWrapperContext(settleContext());

        assertThrows(DuplicateRequestException.class, () -> processor.process(state));

        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publish(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void policyRejection_beforeClaim_writesNoTxnDoc() {
        // Policy is a pure read-gate and now runs BEFORE the claim, so a rejected request never
        // reaches insertOrGet — no doc is persisted, and therefore none has to be marked ERROR to
        // re-arm. A vendor retry deterministically re-hits the same rejection (e.g. a genuinely-new
        // settle on an already-ended round → 9999) instead of colliding on an in-flight NEW doc.
        doThrow(new RoundAlreadyEndedException("round ended"))
                .when(creditPolicy)
                .evaluate(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditConfig.class), ArgumentMatchers.any());

        assertThrows(RoundAlreadyEndedException.class,
                () -> processor.process(new CreditWrapperContext(settleContext())));

        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publish(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void policyRejection_thenLatePeekSuccess_returnsRecordedBalance() {
        // Closes the peek→claim window. A concurrent attempt on THIS txn id completed between the
        // wrapper's peek and the gate — including the very settle that closed the round — so the
        // gate read stale state. A recorded SUCCESS outranks the gate.
        doThrow(new RoundAlreadyEndedException("round ended"))
                .when(creditPolicy)
                .evaluate(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditConfig.class), ArgumentMatchers.any());
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class)))
                .thenReturn(Optional.of(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("106.50"))));

        CreditWrapperContext state = new CreditWrapperContext(settleContext());
        state.getConfig().allowCreditWhenRoundEnded(false).returnSuccessOnDuplicate(true);

        PlayerBalanceData result = processor.process(state);

        assertEquals(0, new BigDecimal("106.50").compareTo(result.getBalance()));
        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
    }

    @Test
    void policyRejection_whenRePeekFails_policyVerdictStands() {
        // The re-peek can only ever UPGRADE the outcome. A KV error there must not substitute a new
        // error for the rejection the vendor should see.
        doThrow(new RoundAlreadyEndedException("round ended"))
                .when(creditPolicy)
                .evaluate(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditConfig.class), ArgumentMatchers.any());
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class)))
                .thenThrow(new RuntimeException("couchbase timeout"));

        assertThrows(RoundAlreadyEndedException.class,
                () -> processor.process(new CreditWrapperContext(settleContext())));

        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
    }

    @Test
    void policyRejection_whenRePeekFindsInFlight_policyVerdictStands() {
        // An in-flight sibling does not change the gate's verdict — swallowed, so the vendor gets
        // the stable policy rejection rather than a duplicate code that depends on timing.
        doThrow(new RoundAlreadyEndedException("round ended"))
                .when(creditPolicy)
                .evaluate(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditConfig.class), ArgumentMatchers.any());
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class)))
                .thenThrow(new DuplicateRequestException("still being processed"));

        assertThrows(RoundAlreadyEndedException.class,
                () -> processor.process(new CreditWrapperContext(settleContext())));
    }

    @Test
    void operatorFailure_runsAuditWithSuccessFalseAndRethrows() {
        CreditTxn txn = freshTxn();
        when(idempotencyResolver.resolve(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.proceed(txn));
        when(operatorInvoker.invoke(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.any(CreditTxn.class),
                ArgumentMatchers.anyLong()))
                .thenThrow(new RuntimeException("operator timed out"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> processor.process(new CreditWrapperContext(settleContext())));

        assertEquals("operator timed out", ex.getMessage());
        ArgumentCaptor<Boolean> succeededCap = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<BigDecimal> balanceCap = ArgumentCaptor.forClass(BigDecimal.class);
        verify(auditPublisher).publish(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.eq(txn),
                ArgumentMatchers.any(CreditConfig.class),
                balanceCap.capture(),
                succeededCap.capture());
        assertEquals(false, succeededCap.getValue(),
                "operatorSucceeded should be false when operator threw");
        assertTrue(balanceCap.getValue() == null,
                "no vendor balance should be passed to audit when operator failed");
    }

    @Test
    void config_carriedFromWrapperToPolicy() {
        CreditWrapperContext wrapper = new CreditWrapperContext(settleContext());
        wrapper.getConfig().allowCreditWhenRoundEnded(false);
        when(idempotencyResolver.resolve(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.proceed(freshTxn()));

        processor.process(wrapper);

        ArgumentCaptor<CreditConfig> cfgCap = ArgumentCaptor.forClass(CreditConfig.class);
        verify(creditPolicy).evaluate(ArgumentMatchers.any(CreditContext.class), cfgCap.capture(), ArgumentMatchers.any());
        assertEquals(false, cfgCap.getValue().isAllowCreditWhenRoundEnded(),
                "policy must see the wrapper's CreditConfig (with vendor overrides)");
    }

    // --- tryReplay: the wrapper's fast path, ahead of all session work ---

    @Test
    void tryReplay_shortCircuitsBeforePolicyGate() {
        // The OVI-133 contract, now enforced at its real location. A SUCCESS replay of the settle
        // that closed the round returns its recorded 0000 balance rather than being rejected by the
        // round-ended gate — and does so before any session lookup, enrichment, or policy read.
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class)))
                .thenReturn(Optional.of(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("106.50"))));
        CreditWrapperContext state = new CreditWrapperContext(settleContext());
        state.getConfig().allowCreditWhenRoundEnded(false).returnSuccessOnDuplicate(true);

        PlayerBalanceData result = processor.tryReplay(state).orElseThrow();

        assertEquals(0, new BigDecimal("106.50").compareTo(result.getBalance()));
        verify(creditPolicy, never()).evaluate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
    }

    @Test
    void tryReplay_absent_returnsEmptyAndTouchesNothing() {
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class))).thenReturn(Optional.empty());

        assertTrue(processor.tryReplay(new CreditWrapperContext(settleContext())).isEmpty());

        verify(creditPolicy, never()).evaluate(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publish(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void tryReplay_withoutReturnSuccessOnDuplicate_throwsDuplicate() {
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class)))
                .thenReturn(Optional.of(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("106.50"))));
        CreditWrapperContext state = new CreditWrapperContext(settleContext());

        assertThrows(DuplicateRequestException.class, () -> processor.tryReplay(state));
    }

    @Test
    void tryReplay_inFlightPeek_propagatesDuplicate() {
        // In-flight collisions must still stop the request — and stop it before session work.
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class)))
                .thenThrow(new DuplicateRequestException("still being processed"));
        CreditWrapperContext state = new CreditWrapperContext(settleContext());

        assertThrows(DuplicateRequestException.class, () -> processor.tryReplay(state));
    }

    @Test
    void tryReplay_peekThrowsKvError_failsOpen() {
        // The peek is an optimisation and the claim is authoritative, so a transient KV error must
        // fall through to the full pipeline. Failing closed would turn an otherwise fine request
        // into a vendor-visible 9999 at the very front of the pipeline, triggering vendor retries.
        when(idempotencyResolver.peek(ArgumentMatchers.any(CreditContext.class)))
                .thenThrow(new RuntimeException("couchbase timeout"));

        assertTrue(processor.tryReplay(new CreditWrapperContext(settleContext())).isEmpty());
    }

    @Test
    void consumedDebitRejection_reachesTheVendor_andNothingIsClaimedCalledOrAudited() {
        // A fresh transferId naming a debit already closed (OVI-2167 / 3185 / 3187) is rejected, not
        // answered as success: no money moves, so 0000 would be a lie. With no recorded result for
        // THIS transferId, the re-peek finds nothing and the rejection stands.
        doThrow(new RollbackNotAllowedException(null, new BetAlreadySettledException("settled")))
                .when(creditPolicy)
                .evaluate(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditConfig.class), ArgumentMatchers.any());

        assertThrows(RollbackNotAllowedException.class,
                () -> processor.process(new CreditWrapperContext(settleContext())));

        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publish(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void process_handsTheWrapperLoadedDebitsToThePolicy() {
        // The wrapper loads the referenced debits once (CreditSessionResolver); the policy must judge
        // those very docs rather than read them a second time.
        when(idempotencyResolver.resolve(ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.proceed(freshTxn()));
        when(operatorInvoker.invoke(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.any(CreditTxn.class),
                ArgumentMatchers.anyLong()))
                .thenReturn(new PlayerBalanceData("bob", "PHP", new BigDecimal("106.50"), 0L));
        CreditWrapperContext state = new CreditWrapperContext(settleContext());
        List<KvDoc<DebitTxn>> loaded = List.of(new KvDoc<>("jdb::DEBIT::100", 1L, new DebitTxn()));
        state.setReferencedDebits(loaded);

        processor.process(state);

        verify(creditPolicy).evaluate(
                ArgumentMatchers.any(CreditContext.class),
                ArgumentMatchers.any(CreditConfig.class),
                ArgumentMatchers.same(loaded));
    }
}
