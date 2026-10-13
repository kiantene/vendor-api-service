package com.nextgen.gameaggregator.core.engine.wallet.credit;

import org.junit.jupiter.api.BeforeEach;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.WalletBetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.WalletTransactionHistoryProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditAuditPublisherTest {

    @Mock private WalletRoundLedgerDataService walletRoundLedgerDataService;
    @Mock private WalletTransactionHistoryProducer walletTransactionHistoryProducer;
    @Mock private WalletBetHistoryProducer walletBetHistoryProducer;
    @Mock private VendorConfigService vendorConfigService;
    @Mock private DebitTxnDataService debitTxnDataService;

    private CreditAuditPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CreditAuditPublisher(walletRoundLedgerDataService, walletTransactionHistoryProducer,
                walletBetHistoryProducer, vendorConfigService, new DebitReferenceGuard(debitTxnDataService));
    }

    private CreditContext settleContext(boolean isRoundEnded) {
        return CreditContext.builder()
                .vendorTxnId("transfer-200")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .isRefund(false)
                .isRoundEnded(isRoundEnded)
                .build();
    }

    private CreditContext refundContext() {
        return CreditContext.builder()
                .vendorTxnId("transfer-300")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .isRefund(true)
                .build();
    }

    private CreditTxn txn(GameRoundState state) {
        CreditTxn t = CreditTxn.of("jdb", "transfer-200", state, 1L);
        t.setVendorPlayerUsername("bob");
        t.setVendorRoundId("round-1");
        return t;
    }

    @Test
    void successAndRoundEnded_appendsRefMarksSettledAndEmits() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);

        publisher.publish(settleContext(true), txn(GameRoundState.SETTLED), new CreditConfig(), new BigDecimal("106.50"), true);

        ArgumentCaptor<WalletRoundTxnRef> refCap = ArgumentCaptor.forClass(WalletRoundTxnRef.class);
        verify(walletRoundLedgerDataService).appendTxnRef(
                ArgumentMatchers.eq("jdb::bob::round-1"),
                refCap.capture(),
                argThat(b -> b != null && b.compareTo(new BigDecimal("106.50")) == 0),
                ArgumentMatchers.eq(17L));
        assertEquals("CREDIT", refCap.getValue().getKind());
        verify(walletRoundLedgerDataService).markSettled("jdb::bob::round-1");
        verify(walletTransactionHistoryProducer).publishTransactionHistoryForCredit(
                ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditTxn.class));
        verify(walletBetHistoryProducer).publishBetHistoryForCredit(
                ArgumentMatchers.any(CreditContext.class), ArgumentMatchers.any(CreditTxn.class));
    }

    @Test
    void roundEndedFalse_skipsMarkSettled() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);

        publisher.publish(settleContext(false), txn(GameRoundState.SETTLED), new CreditConfig(), new BigDecimal("100.00"), true);

        verify(walletRoundLedgerDataService).appendTxnRef(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(walletRoundLedgerDataService, never()).markSettled(ArgumentMatchers.anyString());
    }

    @Test
    void refund_neverMarksSettled() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));

        publisher.publish(refundContext(), txn(GameRoundState.REFUNDED), new CreditConfig(), new BigDecimal("100.00"), true);

        verify(walletRoundLedgerDataService, never()).markSettled(ArgumentMatchers.anyString());
    }

    @Test
    void operatorFailure_appendsRefAndTxnHistoryButEmitsNoBetHistory() {
        // The audit-of-attempt invariant still holds — the ledger ref and the transaction-history
        // record are written on the failure path, both carrying the txn's real status.
        //
        // Bet history must NOT be emitted. publishBetHistoryForCredit derives status from
        // context.isRefund() alone (SETTLED / REFUNDED) and reports the full requested amounts, and
        // BetStatus has no failure value — so emitting here publishes a SETTLED record with a full
        // winLoss for money that never moved. BI joins on status and sums winLoss, so this
        // over-reported settlements on every operator failure.
        //
        // This assertion previously read verify(walletBetHistoryProducer).publishBetHistoryForCredit
        // — i.e. it locked the bug in as intended behaviour through six review passes.
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);

        publisher.publish(settleContext(true), txn(GameRoundState.SETTLED), new CreditConfig(), null, false);

        verify(walletRoundLedgerDataService).appendTxnRef(
                ArgumentMatchers.eq("jdb::bob::round-1"),
                ArgumentMatchers.any(WalletRoundTxnRef.class),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(17L));
        verify(walletRoundLedgerDataService, never()).markSettled(ArgumentMatchers.anyString());
        verify(walletTransactionHistoryProducer).publishTransactionHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(walletBetHistoryProducer, never()).publishBetHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void refundOperatorFailure_emitsNoBetHistory() {
        // Refund mode is the worse half: populateRefundAmounts sets winLoss = betAmount.negate(),
        // which BI sums for net-player-PNL, so a failed refund actively skewed player PNL.
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));

        publisher.publish(refundContext(), txn(GameRoundState.REFUNDED), new CreditConfig(), null, false);

        verify(walletBetHistoryProducer, never()).publishBetHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void ledgerMissing_skipsAppendButStillEmits() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1")).thenReturn(null);
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);

        publisher.publish(settleContext(true), txn(GameRoundState.SETTLED), new CreditConfig(), new BigDecimal("1.00"), true);

        verify(walletRoundLedgerDataService, never()).appendTxnRef(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(walletRoundLedgerDataService).markSettled("jdb::bob::round-1");
        verify(walletTransactionHistoryProducer).publishTransactionHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void transactionHistoryDisabled_skipsTxnHistoryButStillEmitsBetHistory() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(false);

        publisher.publish(settleContext(true), txn(GameRoundState.SETTLED), new CreditConfig(), new BigDecimal("1.00"), true);

        verify(walletTransactionHistoryProducer, never()).publishTransactionHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(walletBetHistoryProducer).publishBetHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void appendRefFails_subsequentStepsStillRun() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);
        doThrow(new RuntimeException("cb unreachable"))
                .when(walletRoundLedgerDataService)
                .appendTxnRef(ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(WalletRoundTxnRef.class),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyLong());

        assertDoesNotThrow(() -> publisher.publish(
                settleContext(true), txn(GameRoundState.SETTLED), new CreditConfig(), new BigDecimal("1.00"), true));
        verify(walletRoundLedgerDataService).markSettled("jdb::bob::round-1");
        verify(walletTransactionHistoryProducer).publishTransactionHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(walletBetHistoryProducer).publishBetHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void kafkaTxnHistoryFails_betHistoryStillEmits() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);
        doThrow(new RuntimeException("kafka down"))
                .when(walletTransactionHistoryProducer)
                .publishTransactionHistoryForCredit(
                        ArgumentMatchers.any(CreditContext.class),
                        ArgumentMatchers.any(CreditTxn.class));

        assertDoesNotThrow(() -> publisher.publish(
                settleContext(true), txn(GameRoundState.SETTLED), new CreditConfig(), new BigDecimal("1.00"), true));
        verify(walletBetHistoryProducer).publishBetHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    // ---------- Referenced-debit consumption marker (OVI-2167 / 3185 / 3187 / 3188) ----------

    private CreditContext boundContext(boolean refund) {
        return CreditContext.builder()
                .vendorTxnId(refund ? "transfer-300" : "transfer-200")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .isRefund(refund)
                .isRoundEnded(false)
                .referencedVendorDebitTxnIds(List.of("transfer-100"))
                .build();
    }

    private CreditConfig bound() {
        return new CreditConfig().requireDebitReference(true);
    }

    private KvDoc<DebitTxn> debitKv() {
        DebitTxn d = new DebitTxn();
        d.setVendorClassName("jdb");
        d.setVendorTxnId("transfer-100");
        return new KvDoc<>("jdb::DEBIT::transfer-100", 7L, d);
    }

    @Test
    void settleSuccess_marksReferencedDebitSettled() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(debitKv());

        publisher.publish(boundContext(false), txn(GameRoundState.SETTLED), bound(),
                new BigDecimal("100.00"), true);

        verify(debitTxnDataService).casUpdateState(
                ArgumentMatchers.any(), ArgumentMatchers.eq(GameRoundState.SETTLED));
    }

    @Test
    void refundSuccess_marksReferencedDebitRefunded() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(debitKv());

        publisher.publish(boundContext(true), txn(GameRoundState.REFUNDED), bound(),
                new BigDecimal("100.00"), true);

        verify(debitTxnDataService).casUpdateState(
                ArgumentMatchers.any(), ArgumentMatchers.eq(GameRoundState.REFUNDED));
    }

    @Test
    void operatorFailed_doesNotMarkTheDebit() {
        // The money never moved, so the debit must stay spendable — otherwise the vendor's retry
        // is answered as a no-op and the payout is lost.
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));

        publisher.publish(boundContext(false), txn(GameRoundState.SETTLED), bound(), null, false);

        verify(debitTxnDataService, never()).casUpdateState(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void defaultConfig_stillMarksTheDebitItNames() {
        // Marking is not opt-in either: whether the reference is required changes only what a
        // credit naming NO debit gets. A named debit is always closed once the operator has paid.
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        KvDoc<DebitTxn> debitKv = new KvDoc<>("jdb::DEBIT::transfer-100", 7L, new DebitTxn());
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(debitKv);

        publisher.publish(boundContext(false), txn(GameRoundState.SETTLED), new CreditConfig(),
                new BigDecimal("100.00"), true);

        verify(debitTxnDataService).casUpdateState(debitKv, GameRoundState.SETTLED);
    }

    @Test
    void markerFailure_doesNotSkipTheLedgerAppendOrTheEmits() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(debitKv());
        doThrow(new RuntimeException("cas exhausted"))
                .when(debitTxnDataService)
                .casUpdateState(ArgumentMatchers.any(), ArgumentMatchers.any());
        when(vendorConfigService.isTransactionHistoryEnabled("jdb")).thenReturn(true);

        assertDoesNotThrow(() -> publisher.publish(boundContext(false), txn(GameRoundState.SETTLED),
                bound(), new BigDecimal("100.00"), true));

        verify(walletRoundLedgerDataService).appendTxnRef(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(WalletRoundTxnRef.class),
                ArgumentMatchers.any(), ArgumentMatchers.anyLong());
        verify(walletTransactionHistoryProducer).publishTransactionHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(walletBetHistoryProducer).publishBetHistoryForCredit(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void missingDebitDoc_isLoggedNotThrown() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1"))
                .thenReturn(new KvDoc<>("jdb::bob::round-1", 17L, new WalletRoundLedger()));
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(null);

        assertDoesNotThrow(() -> publisher.publish(boundContext(false), txn(GameRoundState.SETTLED),
                bound(), new BigDecimal("100.00"), true));

        verify(debitTxnDataService, never()).casUpdateState(
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }
}
