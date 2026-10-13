package com.nextgen.gameaggregator.core.engine.wallet.credit;

import org.junit.jupiter.api.BeforeEach;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetAlreadySettledException;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.BetResultRejectedException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyRefundedException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyEndedException;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditPolicyTest {

    @Mock private WalletRoundLedgerDataService walletRoundLedgerDataService;
    @Mock private DebitTxnDataService debitTxnDataService;

    private CreditPolicy policy;
    private DebitReferenceGuard debitReferenceGuard;

    @BeforeEach
    void setUp() {
        // A real binder over the mocked data service: the binding rules moved out of CreditPolicy,
        // and these tests pin them through the policy exactly as before.
        debitReferenceGuard = new DebitReferenceGuard(debitTxnDataService);
        policy = new CreditPolicy(walletRoundLedgerDataService, debitReferenceGuard);
    }

    /** As the wrapper runs it: the referenced debits are loaded once, then handed to the policy. */
    private void evaluate(CreditContext context, CreditConfig config) {
        policy.evaluate(context, config, debitReferenceGuard.load(context));
    }

    private CreditContext settle(boolean refund, List<String> refDebitIds) {
        return CreditContext.builder()
                .vendorTxnId("transfer-200")
                .vendorClassName("jdb")
                .vendorPlayerUsername("bob")
                .vendorRoundId("round-1")
                .isRefund(refund)
                .referencedVendorDebitTxnIds(refDebitIds)
                .build();
    }

    private WalletRoundLedger ledger(boolean ended) {
        WalletRoundLedger ledger = new WalletRoundLedger();
        ledger.setIsEnded(ended);
        ledger.setTransactions(new ArrayList<>());
        return ledger;
    }

    private KvDoc<WalletRoundLedger> kv(WalletRoundLedger l) {
        return new KvDoc<>("jdb::bob::round-1", 1L, l);
    }

    // ---------- Settle mode ----------

    @Test
    void settle_defaultFlags_skipsLedgerLookup() {
        CreditConfig cfg = new CreditConfig(); // default: allowCreditWhenRoundEnded=true
        assertDoesNotThrow(() -> evaluate(settle(false, null), cfg));
        verify(walletRoundLedgerDataService, never()).findById(ArgumentMatchers.anyString());
    }

    @Test
    void settle_roundEnded_false_throwsWhenLedgerIsEnded() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1")).thenReturn(kv(ledger(true)));
        CreditConfig cfg = new CreditConfig().allowCreditWhenRoundEnded(false);
        assertThrows(RoundAlreadyEndedException.class, () -> evaluate(settle(false, null), cfg));
    }

    @Test
    void settle_roundEnded_false_passesWhenLedgerNotEnded() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1")).thenReturn(kv(ledger(false)));
        CreditConfig cfg = new CreditConfig().allowCreditWhenRoundEnded(false);
        assertDoesNotThrow(() -> evaluate(settle(false, null), cfg));
    }

    @Test
    void settle_roundEnded_false_passesWhenLedgerMissing() {
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1")).thenReturn(null);
        CreditConfig cfg = new CreditConfig().allowCreditWhenRoundEnded(false);
        assertDoesNotThrow(() -> evaluate(settle(false, null), cfg));
    }

    // ---------- Refund mode ----------

    @Test
    void refund_throwsWhenNoUpstreamDebit() {
        // A refund that names no debit has nothing to refund (OVI-3184).
        CreditConfig cfg = new CreditConfig();
        assertThrows(BetNotFoundException.class, () -> evaluate(settle(true, null), cfg));
    }

    @Test
    void refund_throwsWhenReferencedDebitMissing() {
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(null);
        CreditConfig cfg = new CreditConfig();
        assertThrows(BetNotFoundException.class,
                () -> evaluate(settle(true, List.of("transfer-100")), cfg));
    }

    @Test
    void refund_throwsWhenReferencedDebitNotSuccess() {
        DebitTxn errored = new DebitTxn();
        errored.setStatus(TxnStatus.ERROR);
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100"))
                .thenReturn(new KvDoc<>("jdb::DEBIT::transfer-100", 7L, errored));
        CreditConfig cfg = new CreditConfig();
        assertThrows(BetNotFoundException.class,
                () -> evaluate(settle(true, List.of("transfer-100")), cfg));
    }

    @Test
    void refund_passesWhenReferencedDebitsAllSuccess() {
        // A real debit: its round and an open state. The debit check now runs for every vendor
        // that names a debit, so the fixture must look like one.
        DebitTxn ok = debit("round-1", GameRoundState.UNSETTLED, "100");
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100"))
                .thenReturn(new KvDoc<>("jdb::DEBIT::transfer-100", 7L, ok));
        CreditConfig cfg = new CreditConfig();
        assertDoesNotThrow(() -> evaluate(settle(true, List.of("transfer-100")), cfg));
    }

    // ---------- Referenced-debit binding (OVI-2167 / 3185 / 3187 / 3188) ----------
    //
    // The credit pipeline's only idempotency key is the credit's OWN vendorTxnId, so a repeat
    // bearing a fresh transfer id reads as a brand-new request. These bind it to the debits it
    // names instead.

    private DebitTxn debit(String roundId, GameRoundState state, String balance) {
        DebitTxn d = new DebitTxn();
        d.setVendorClassName("jdb");
        d.setVendorPlayerUsername("bob");
        d.setVendorRoundId(roundId);
        d.setStatus(TxnStatus.SUCCESS);
        d.setState(state);
        d.setVendorBalance(new BigDecimal(balance));
        return d;
    }

    private void debitAt(String vendorTxnId, DebitTxn d) {
        when(debitTxnDataService.findById("jdb::DEBIT::" + vendorTxnId))
                .thenReturn(new KvDoc<>("jdb::DEBIT::" + vendorTxnId, 7L, d));
    }

    private CreditConfig bound() {
        return new CreditConfig().requireDebitReference(true);
    }

    @Test
    void defaultConfig_stillChecksADebitThatIsNamed() {
        // Checking a named debit is not opt-in: a vendor that is not required to send a debit
        // reference is still protected when it does. Here the named debit does not exist.
        CreditConfig cfg = new CreditConfig();
        assertThrows(BetNotFoundException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), cfg));
    }

    @Test
    void settleNamingNoDebit_readsNoDebit() {
        assertDoesNotThrow(() -> evaluate(settle(false, null), new CreditConfig()));
        verify(debitTxnDataService, never()).findById(ArgumentMatchers.anyString());
    }

    @Test
    void binding_unconsumedDebitOnSameRound_proceeds() {
        debitAt("transfer-100", debit("round-1", GameRoundState.UNSETTLED, "100"));
        assertDoesNotThrow(() -> evaluate(settle(false, List.of("transfer-100")), bound()));
    }

    // A request under a FRESH transferId naming a debit already closed moves no money, so it is
    // rejected: 0000 means the credit was applied. Only a same-transferId replay answers 0000, and
    // that never reaches the policy (the wrapper's peek answers it).

    @Test
    void ovi3187_settleAfterCancel_isRejectedAsAlreadyRefunded() {
        debitAt("transfer-100", debit("round-1", GameRoundState.REFUNDED, "1"));

        BetResultRejectedException ex = assertThrows(BetResultRejectedException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), bound()));

        assertTrue(ex.isRoundAlreadyRefunded());
    }

    @Test
    void settleAfterSettle_sameRound_isRejectedAsAlreadySettled() {
        debitAt("transfer-100", debit("round-1", GameRoundState.SETTLED, "1"));

        BetResultRejectedException ex = assertThrows(BetResultRejectedException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), bound()));

        assertInstanceOf(BetAlreadySettledException.class, ex.getCause());
    }

    @Test
    void ovi3185_settleOfASettledDebitUnderADifferentRound_isRejectedAsAlreadySettled() {
        // Both a spent debit AND a round mismatch. The consumed check runs first, so the cause names
        // the real reason (already settled) rather than the round mismatch. Both are 9999 on the wire.
        debitAt("transfer-100", debit("round-A", GameRoundState.SETTLED, "1"));

        BetResultRejectedException ex = assertThrows(BetResultRejectedException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), bound()));

        assertInstanceOf(BetAlreadySettledException.class, ex.getCause());
    }

    @Test
    void ovi2167_cancelAfterCancel_isRejectedAsAlreadyRefunded() {
        // Second cancel arrives with a FRESH transferId, so the credit-side idempotency key differs
        // and cannot see it. The debit's own state is the only thing both requests share.
        debitAt("transfer-100", debit("round-1", GameRoundState.REFUNDED, "1"));

        RollbackNotAllowedException ex = assertThrows(RollbackNotAllowedException.class,
                () -> evaluate(settle(true, List.of("transfer-100")), bound()));

        assertInstanceOf(RoundAlreadyRefundedException.class, ex.getCause());
    }

    @Test
    void cancelAfterSettle_isRejectedAsBetAlreadySettled() {
        debitAt("transfer-100", debit("round-1", GameRoundState.SETTLED, "1"));

        RollbackNotAllowedException ex = assertThrows(RollbackNotAllowedException.class,
                () -> evaluate(settle(true, List.of("transfer-100")), bound()));

        assertTrue(ex.isBetAlreadySettled());
    }

    @Test
    void consumedDebits_settledOutranksRefundedInTheCause() {
        debitAt("transfer-100", debit("round-1", GameRoundState.REFUNDED, "1"));
        debitAt("transfer-200", debit("round-1", GameRoundState.SETTLED, "1"));

        BetResultRejectedException ex = assertThrows(BetResultRejectedException.class,
                () -> evaluate(settle(false, List.of("transfer-100", "transfer-200")), bound()));

        assertInstanceOf(BetAlreadySettledException.class, ex.getCause());
    }

    @Test
    void cancelNamingOneSettledDebitAmongOpenOnes_isRejected() {
        debitAt("transfer-100", debit("round-1", GameRoundState.UNSETTLED, "1"));
        debitAt("transfer-200", debit("round-1", GameRoundState.SETTLED, "1"));

        assertThrows(RollbackNotAllowedException.class,
                () -> evaluate(settle(true, List.of("transfer-100", "transfer-200")), bound()));
    }

    @Test
    void binding_partialOverlap_rejectsRatherThanPayingOutTheRemainder() {
        debitAt("transfer-100", debit("round-1", GameRoundState.SETTLED, "1"));

        // transfer-200 is loaded like every ref (loadReferencedDebits reads them all up front), and
        // it is missing. Had the resolvable check run first, this would be BetNotFoundException — so
        // BetResultRejectedException is what proves the consumed check ran ahead of it.
        assertThrows(BetResultRejectedException.class,
                () -> evaluate(settle(false, List.of("transfer-100", "transfer-200")), bound()));
        verify(debitTxnDataService).findById("jdb::DEBIT::transfer-200");
    }

    @Test
    void binding_rejectionReadsNoLedger() {
        // With no answer balance to look up any more, rejecting a consumed debit costs only the
        // debit reads (default allowCreditWhenRoundEnded=true, so no round-ended ledger read either).
        debitAt("transfer-100", debit("round-1", GameRoundState.SETTLED, "1"));

        assertThrows(BetResultRejectedException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), bound()));
        verify(walletRoundLedgerDataService, never()).findById(ArgumentMatchers.anyString());
    }

    @Test
    void consumedDebitOnAnEndedRound_isRejected() {
        // With the round-ended gate on, the gate fires first. Same wire answer (9999) as the
        // consumed rejection; which one fires does not change the response.
        debitAt("transfer-100", debit("round-1", GameRoundState.SETTLED, "1"));
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1")).thenReturn(kv(ledger(true)));
        CreditConfig cfg = bound().allowCreditWhenRoundEnded(false);

        assertThrows(RoundAlreadyEndedException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), cfg));
    }

    @Test
    void unconsumedDebitOnAnEndedRound_isRejectedByTheGate() {
        debitAt("transfer-100", debit("round-1", GameRoundState.UNSETTLED, "1"));
        when(walletRoundLedgerDataService.findById("jdb::bob::round-1")).thenReturn(kv(ledger(true)));
        CreditConfig cfg = bound().allowCreditWhenRoundEnded(false);

        assertThrows(RoundAlreadyEndedException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), cfg));
    }

    @Test
    void ovi3188_twoDebitsFromDifferentRounds_throwsBetNotFound() {
        debitAt("transfer-100", debit("round-1", GameRoundState.UNSETTLED, "1"));
        debitAt("transfer-200", debit("round-2", GameRoundState.UNSETTLED, "1"));

        assertThrows(BetNotFoundException.class,
                () -> evaluate(settle(false, List.of("transfer-100", "transfer-200")), bound()));
    }

    @Test
    void binding_missingDebit_throwsBetNotFound() {
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(null);
        assertThrows(BetNotFoundException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), bound()));
    }

    @Test
    void binding_debitNotSuccess_throwsBetNotFound() {
        DebitTxn errored = debit("round-1", GameRoundState.UNSETTLED, "1");
        errored.setStatus(TxnStatus.ERROR);
        debitAt("transfer-100", errored);
        assertThrows(BetNotFoundException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), bound()));
    }

    @Test
    void binding_refundOfAMissingDebit_isBetNotFound_ovi3184() {
        // OVI-3184: a cancel naming a debit GA never recorded. BetNotFound, which JDB maps to 9999.
        when(debitTxnDataService.findById("jdb::DEBIT::transfer-100")).thenReturn(null);
        assertThrows(BetNotFoundException.class,
                () -> evaluate(settle(true, List.of("transfer-100")), bound()));
    }

    @Test
    void binding_refundModeLoadsEachDebitOnce_notOncePerCheck() {
        debitAt("transfer-100", debit("round-1", GameRoundState.UNSETTLED, "1"));
        assertDoesNotThrow(() -> evaluate(settle(true, List.of("transfer-100")), bound()));
        verify(debitTxnDataService).findById("jdb::DEBIT::transfer-100");
    }

    // ---------- requireDebitReference ----------

    @Test
    void required_settleNamingNoDebit_isInvalidRequest_beforeAnyRead() {
        CreditConfig cfg = bound();
        assertThrows(InvalidRequestException.class, () -> evaluate(settle(false, null), cfg));
        assertThrows(InvalidRequestException.class, () -> evaluate(settle(false, List.of()), cfg));
        verify(debitTxnDataService, never()).findById(ArgumentMatchers.anyString());
    }

    @Test
    void required_refundNamingNoDebit_isInvalidRequest_notBetNotFound() {
        // A request-shape error outranks the refund state check, so it maps to the vendor's
        // invalid-request code (JDB 8000) rather than BetNotFound (9999).
        assertThrows(InvalidRequestException.class, () -> evaluate(settle(true, null), bound()));
    }

    @Test
    void notRequired_settleNamingNoDebit_proceeds() {
        assertDoesNotThrow(() -> evaluate(settle(false, null), new CreditConfig()));
    }

    @Test
    void notRequired_settleNamingAConsumedDebit_isStillRejected() {
        debitAt("transfer-100", debit("round-1", GameRoundState.SETTLED, "1"));

        assertThrows(BetResultRejectedException.class,
                () -> evaluate(settle(false, List.of("transfer-100")), new CreditConfig()));
    }
}
