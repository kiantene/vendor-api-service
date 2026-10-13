package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundTxnRef;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.WalletBetHistoryProducer;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.WalletTransactionHistoryProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Runs the credit pipeline's audit-of-attempt side-effects. Called from the
 * orchestrator's {@code finally} block — must execute on BOTH success and
 * failure paths. Each side-effect is independently wrapped so a downstream
 * failure does not skip the next step and does not mask the operator outcome.
 *
 * <p>Side-effect order:
 * <ol>
 *   <li>{@link DebitReferenceGuard#markConsumed} — only when {@code operatorSucceeded}, and a
 *       no-op when the credit names no debit. First, because it is the only
 *       correctness-bearing step here: it is what stops a later credit spending the same debit
 *       again.</li>
 *   <li>{@code appendTxnRef} — always (status captured in the ref itself).</li>
 *   <li>{@code markSettled} — only when {@code operatorSucceeded && !isRefund && isRoundEnded}.</li>
 *   <li>Kafka transaction history — gated by {@link VendorConfigService#isTransactionHistoryEnabled}.
 *       Emitted on both paths: it is a transaction record and carries the txn's own status.</li>
 *   <li>Kafka bet history — only when {@code operatorSucceeded}. It reports a bet OUTCOME with a
 *       status drawn from the request mode, and {@code BetStatus} has no failure value, so emitting
 *       on the failure path would mislabel money that never moved. Matches the debit side.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditAuditPublisher {

    private final WalletRoundLedgerDataService walletRoundLedgerDataService;
    private final WalletTransactionHistoryProducer walletTransactionHistoryProducer;
    private final WalletBetHistoryProducer walletBetHistoryProducer;
    private final VendorConfigService vendorConfigService;
    private final DebitReferenceGuard debitReferenceGuard;

    public void publish(CreditContext context,
                        CreditTxn credit,
                        CreditConfig config,
                        BigDecimal vendorLastBalance,
                        boolean operatorSucceeded) {
        if (operatorSucceeded) {
            debitReferenceGuard.markConsumed(context);
        }

        appendLedgerRef(credit, vendorLastBalance);

        if (operatorSucceeded
                && !context.isRefund()
                && Boolean.TRUE.equals(context.getIsRoundEnded())) {
            tryMarkSettled(credit.getRoundDocId());
        }

        emitTransactionHistory(context, credit);

        // Bet history is success-only, mirroring DebitAuditPublisher.publishSuccess.
        //
        // It used to emit unconditionally ("legacy parity"), which silently corrupted BI.
        // publishBetHistoryForCredit derives status from context.isRefund() ALONE — SETTLED or
        // REFUNDED, with the full requested betAmount/winAmount/winLoss — so a failed settle
        // emitted a fully-formed SETTLED record for money that never moved. BI joins on status and
        // sums winLoss for net-player-PNL, so every operator failure over-reported settlements, and
        // a failed refund over-reported refund PNL via winLoss = betAmount.negate().
        //
        // There is no honest status to emit instead: BetStatus has only UNSETTLED / SETTLED /
        // CANCELLED / REFUNDED — no failure state. So the record is dropped rather than
        // mislabelled. The attempt is still audited: appendLedgerRef above records the ref with the
        // txn's real status, and the CreditTxn doc itself carries ERROR + the exception.
        if (operatorSucceeded) {
            emitBetHistory(context, credit);
        }
    }

    private void appendLedgerRef(CreditTxn credit, BigDecimal vendorLastBalance) {
        try {
            KvDoc<WalletRoundLedger> ledgerKv = walletRoundLedgerDataService.findById(credit.getRoundDocId());
            if (ledgerKv == null) {
                log.warn("WalletRoundLedger missing for credit {} — skipping appendTxnRef",
                        credit.getId());
                return;
            }
            walletRoundLedgerDataService.appendTxnRef(
                    ledgerKv.getId(),
                    WalletRoundTxnRef.of(credit),
                    vendorLastBalance,
                    ledgerKv.getCas());
        } catch (RuntimeException ex) {
            log.error("Failed to appendTxnRef for credit {}: {}",
                    credit.getId(), ex.getMessage(), ex);
        }
    }

    private void tryMarkSettled(String ledgerId) {
        try {
            walletRoundLedgerDataService.markSettled(ledgerId);
        } catch (RuntimeException ex) {
            log.error("Failed to markSettled ledger {}: {}", ledgerId, ex.getMessage(), ex);
        }
    }

    private void emitTransactionHistory(CreditContext context, CreditTxn credit) {
        if (!vendorConfigService.isTransactionHistoryEnabled(context.getVendorClassName())) {
            return;
        }
        try {
            walletTransactionHistoryProducer.publishTransactionHistoryForCredit(context, credit);
        } catch (RuntimeException ex) {
            log.error("Failed to publish credit transaction history for {}: {}",
                    credit.getId(), ex.getMessage(), ex);
        }
    }

    private void emitBetHistory(CreditContext context, CreditTxn credit) {
        try {
            walletBetHistoryProducer.publishBetHistoryForCredit(context, credit);
        } catch (RuntimeException ex) {
            log.error("Failed to publish credit bet history for {}: {}",
                    credit.getId(), ex.getMessage(), ex);
        }
    }
}
