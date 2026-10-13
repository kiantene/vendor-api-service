package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyEndedException;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import com.nextgen.gameaggregator.service.data.WalletRoundLedgerDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pre-flight gates for the credit pipeline. Evaluates {@link CreditConfig} flags
 * against persisted state and rejects before any operator call or doc mutation. There is no
 * "succeed without moving money" outcome: a credit either proceeds to the operator or is rejected.
 * The only success that moves no money is a replay of the <i>same</i> {@code vendorTxnId}, and
 * that is answered by the idempotency layer, never here:
 *
 * <ul>
 *   <li><b>Settle mode</b> ({@code !isRefund}) — {@code allowCreditWhenRoundEnded=false} +
 *       ledger {@code isEnded=true} → {@link RoundAlreadyEndedException}.</li>
 *   <li><b>Refund mode</b> ({@code isRefund}) — always: no referenced debit txn ids →
 *       {@link BetNotFoundException}. A refund refunds a debit, so one that names none has nothing
 *       to refund (OVI-3184).</li>
 *   <li><b>Both modes</b> — a credit naming debits is checked against them, and one naming none
 *       is rejected if the vendor requires a debit reference; see {@link DebitReferenceGuard}.</li>
 * </ul>
 *
 * <p>The binding itself — which debits a credit closes, and whether it may — lives in
 * {@link DebitReferenceGuard}, which also marks them closed after the operator call. This class
 * only decides the order the checks run in, and loads the debits once for both.
 */
@Service
@RequiredArgsConstructor
public class CreditPolicy {

    private final WalletRoundLedgerDataService walletRoundLedgerDataService;
    private final DebitReferenceGuard debitReferenceGuard;

    /**
     * Returns normally to proceed.
     *
     * @param referenced the credit's referenced debits, loaded once by the wrapper
     *                   ({@link CreditWrapperContext#getReferencedDebits()}); empty when it names none
     * @throws RuntimeException when the credit is rejected; see {@link DebitReferenceGuard#check}
     */
    public void evaluate(CreditContext context, CreditConfig config, List<KvDoc<DebitTxn>> referenced) {
        debitReferenceGuard.requirePresent(context, config.isRequireDebitReference());

        if (context.isRefund()) {
            evaluateRefund(context);
        } else {
            evaluateSettle(context, config);
        }
        debitReferenceGuard.check(context, referenced);
    }

    private void evaluateSettle(CreditContext context, CreditConfig config) {
        if (config.isAllowCreditWhenRoundEnded()) {
            return;
        }

        String ledgerId = WalletRoundLedger.createDocId(
                context.getVendorClassName(),
                context.getVendorPlayerUsername(),
                context.getVendorRoundId());
        KvDoc<WalletRoundLedger> ledgerKv = walletRoundLedgerDataService.findById(ledgerId);
        if (ledgerKv != null && Boolean.TRUE.equals(ledgerKv.getPayload().getIsEnded())) {
            throw new RoundAlreadyEndedException(
                    "Credit rejected: round " + ledgerId + " already ended");
        }
    }

    /**
     * A refund refunds a debit, so one that names none has nothing to refund (OVI-3184). Whether
     * each named debit exists, succeeded and is still open is {@link DebitReferenceGuard#check}'s
     * job, which runs for every credit that names debits.
     *
     * <p>{@link BetNotFoundException}: the bet this refund would reverse cannot be found. Mapped
     * by each vendor's {@code onBetNotFound} (JDB {@code 9999}).
     */
    private void evaluateRefund(CreditContext context) {
        List<String> refs = context.getReferencedVendorDebitTxnIds();
        if (refs == null || refs.isEmpty()) {
            throw new BetNotFoundException("Refund rejected: no referenced debit txn ids supplied");
        }
    }
}
