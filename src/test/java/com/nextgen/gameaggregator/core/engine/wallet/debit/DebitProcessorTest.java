package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orchestration-level coverage for {@link DebitProcessor}. Inner-phase behavior
 * is covered by the per-collaborator test classes.
 */
@ExtendWith(MockitoExtension.class)
class DebitProcessorTest {

    @Mock private DebitIdempotencyResolver idempotencyResolver;
    @Mock private DebitLedgerInitializer ledgerInitializer;
    @Mock private DebitOperatorInvoker operatorInvoker;
    @Mock private DebitAuditPublisher auditPublisher;

    @InjectMocks private DebitProcessor processor;

    private DebitContext sampleContext() {
        return DebitContext.builder()
                .traceId("trace-1")
                .idempotencyKey("transfer-1")
                .vendorTxnId("transfer-1")
                .vendorRoundId("round-1")
                .vendorAmount(new BigDecimal("5.00"))
                .vendorTimestamp(1700000000000L)
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorCurrency("PHP")
                .currencyCode("PHP")
                .vendorGameCode("SG12Zodiacs")
                .gameCode("SG12Zodiacs")
                .token("session-token")
                .vendorId(88)
                .agentId(1)
                .agentPlayerUsername("op-bob")
                .currencyId(7)
                .fromVendorRate(BigDecimal.ONE)
                .toVendorRate(BigDecimal.ONE)
                .build();
    }

    private DebitTxn freshTxn() {
        DebitTxn txn = DebitTxn.of("jdb", "transfer-1", 1L);
        txn.setVendorPlayerUsername("bob");
        txn.setVendorRoundId("round-1");
        return txn;
    }

    @Test
    void happyPath_invokesOperatorAndPublishesAudit() {
        DebitTxn txn = freshTxn();
        KvDoc<WalletRoundLedger> ledgerKv = new KvDoc<>("jdb::bob::round-1", 42L, new WalletRoundLedger());
        when(idempotencyResolver.resolve(ArgumentMatchers.any(DebitContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.proceed(txn));
        when(ledgerInitializer.ensure(ArgumentMatchers.any(DebitContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(ledgerKv);
        when(operatorInvoker.invoke(
                ArgumentMatchers.any(DebitContext.class),
                ArgumentMatchers.eq(txn),
                ArgumentMatchers.anyLong()))
                .thenReturn(new PlayerBalanceData("bob", "PHP", new BigDecimal("95.00"), 0L));

        PlayerBalanceData result = processor.process(new DebitWrapperContext(sampleContext()));

        assertNotNull(result);
        assertEquals(0, new BigDecimal("95.00").compareTo(result.getBalance()));
        verify(auditPublisher).publishSuccess(
                ArgumentMatchers.any(DebitContext.class),
                ArgumentMatchers.eq(txn),
                ArgumentMatchers.argThat(b -> b != null && b.compareTo(new BigDecimal("95.00")) == 0),
                ArgumentMatchers.eq(ledgerKv));
    }

    @Test
    void replayWithReturnSuccessOnDuplicate_returnsPriorBalanceAndSkipsSideEffects() {
        when(idempotencyResolver.resolve(ArgumentMatchers.any(DebitContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("95.00")));

        DebitWrapperContext state = new DebitWrapperContext(sampleContext());
        state.getConfig().returnSuccessOnDuplicate(true);

        PlayerBalanceData result = processor.process(state);

        assertEquals(0, new BigDecimal("95.00").compareTo(result.getBalance()));
        verify(ledgerInitializer, never()).ensure(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publishSuccess(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void replayWithoutReturnSuccessOnDuplicate_throwsDuplicateRequestAndSkipsSideEffects() {
        when(idempotencyResolver.resolve(ArgumentMatchers.any(DebitContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("95.00")));

        // Default config.returnSuccessOnDuplicate == false — vendor wants the wire-level
        // DUPLICATE_TRANSACTION code, not a silent dedupe.
        DebitWrapperContext state = new DebitWrapperContext(sampleContext());

        assertThrows(DuplicateRequestException.class, () -> processor.process(state));

        verify(ledgerInitializer, never()).ensure(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publishSuccess(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void operatorFailure_propagatesAndSkipsAudit() {
        DebitTxn txn = freshTxn();
        when(idempotencyResolver.resolve(ArgumentMatchers.any(DebitContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(WalletIdempotencyResolver.Resolution.proceed(txn));
        when(ledgerInitializer.ensure(ArgumentMatchers.any(DebitContext.class), ArgumentMatchers.anyLong()))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 42L, new WalletRoundLedger()));
        when(operatorInvoker.invoke(
                ArgumentMatchers.any(DebitContext.class),
                ArgumentMatchers.any(DebitTxn.class),
                ArgumentMatchers.anyLong()))
                .thenThrow(new RuntimeException("operator down"));

        assertThrows(RuntimeException.class,
                () -> processor.process(new DebitWrapperContext(sampleContext())));

        // Failed debit: no audit — "failed bet = no bet".
        verify(auditPublisher, never()).publishSuccess(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    // --- tryReplay: the wrapper's fast path, ahead of all session work ---

    @Test
    void tryReplay_success_returnsPriorBalanceAndTouchesNothing() {
        // A replay of an already-completed debit must not run the session lookup that can now throw
        // (an Action 9 replay after session TTL expiry currently returns 7501 for a transaction GA
        // has already debited) nor the enrichment chain.
        when(idempotencyResolver.peek(ArgumentMatchers.any(DebitContext.class)))
                .thenReturn(Optional.of(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("95.00"))));
        DebitWrapperContext state = new DebitWrapperContext(sampleContext());
        state.getConfig().returnSuccessOnDuplicate(true);

        PlayerBalanceData result = processor.tryReplay(state).orElseThrow();

        assertEquals(0, new BigDecimal("95.00").compareTo(result.getBalance()));
        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(ledgerInitializer, never()).ensure(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(auditPublisher, never()).publishSuccess(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void tryReplay_absent_returnsEmptyAndTouchesNothing() {
        when(idempotencyResolver.peek(ArgumentMatchers.any(DebitContext.class))).thenReturn(Optional.empty());

        assertTrue(processor.tryReplay(new DebitWrapperContext(sampleContext())).isEmpty());

        verify(idempotencyResolver, never()).resolve(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(ledgerInitializer, never()).ensure(ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(operatorInvoker, never()).invoke(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyLong());
    }

    @Test
    void tryReplay_withoutReturnSuccessOnDuplicate_throwsDuplicate() {
        when(idempotencyResolver.peek(ArgumentMatchers.any(DebitContext.class)))
                .thenReturn(Optional.of(WalletIdempotencyResolver.Resolution.replay(new BigDecimal("95.00"))));
        DebitWrapperContext state = new DebitWrapperContext(sampleContext());

        assertThrows(DuplicateRequestException.class, () -> processor.tryReplay(state));
    }

    @Test
    void tryReplay_inFlightPeek_propagatesDuplicate() {
        // In-flight collisions must still stop the request — and stop it before session work.
        when(idempotencyResolver.peek(ArgumentMatchers.any(DebitContext.class)))
                .thenThrow(new DuplicateRequestException("still being processed"));
        DebitWrapperContext state = new DebitWrapperContext(sampleContext());

        assertThrows(DuplicateRequestException.class, () -> processor.tryReplay(state));
    }

    @Test
    void tryReplay_peekThrowsKvError_failsOpen() {
        // The peek is an optimisation and the claim is authoritative, so a transient KV error must
        // fall through to the full pipeline rather than becoming a vendor-visible error.
        when(idempotencyResolver.peek(ArgumentMatchers.any(DebitContext.class)))
                .thenThrow(new RuntimeException("couchbase timeout"));

        assertTrue(processor.tryReplay(new DebitWrapperContext(sampleContext())).isEmpty());
    }
}
