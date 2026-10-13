package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.WalletTransactionHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Runs the debit pipeline's success-only audit side-effects:
 * {@code appendTxnRef} (CAS-aware) followed by Kafka transaction-history emit
 * (gated by {@link VendorConfigService#isTransactionHistoryEnabled}).
 *
 * <p>Unlike the credit-side publisher, debit's audit fires only on success —
 * failed-debit semantics ("failed bet = no bet") deliberately persist nothing.
 * The operator wallet is already debited by the time this runs, so each
 * side-effect ({@code appendTxnRef} and the Kafka emit) is independently wrapped:
 * a downstream write failure must not mask the successful operator outcome or
 * surface a false failure to the vendor. Mirrors {@code CreditAuditPublisher}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebitAuditPublisher {

    private final WalletRoundLedgerDataService walletRoundLedgerDataService;
    private final WalletTransactionHistoryProducer walletTransactionHistoryProducer;
    private final VendorConfigService vendorConfigService;

    public void publishSuccess(DebitContext context,
                               DebitTxn debit,
                               BigDecimal vendorBalance,
                               KvDoc<WalletRoundLedger> ledgerKv) {
        appendLedgerRef(debit, vendorBalance, ledgerKv);
        emitTransactionHistory(context, debit);
    }

    private void appendLedgerRef(DebitTxn debit, BigDecimal vendorBalance,
                                 KvDoc<WalletRoundLedger> ledgerKv) {
        // Best-effort audit — mirrors CreditAuditPublisher.appendLedgerRef. By this point the
        // operator wallet is already debited and the txn is marked SUCCESS, so a ledger-ref write
        // failure must NOT propagate to the vendor: doing so surfaces a false failure and invites a
        // retry that double-debits. The ref is a snapshot, not the source of truth — log and move on.
        try {
            walletRoundLedgerDataService.appendTxnRef(
                    ledgerKv != null ? ledgerKv.getId() : debit.getRoundDocId(),
                    WalletRoundTxnRef.of(debit),
                    vendorBalance,
                    ledgerKv != null ? ledgerKv.getCas() : 0L);
        } catch (RuntimeException ex) {
            log.error("Failed to appendTxnRef for debit {}: {}", debit.getId(), ex.getMessage(), ex);
        }
    }

    private void emitTransactionHistory(DebitContext context, DebitTxn debit) {
        if (!vendorConfigService.isTransactionHistoryEnabled(context.getVendorClassName())) {
            return;
        }
        try {
            walletTransactionHistoryProducer.publishTransactionHistoryForDebit(context, debit);
        } catch (RuntimeException ex) {
            log.error("Failed to publish debit transaction history for {}: {}",
                    debit.getId(), ex.getMessage(), ex);
        }
    }
}
