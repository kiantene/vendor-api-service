package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Persistence facade for {@link CreditTxn} standalone Couchbase documents in the
 * {@code wallet_round_transactions} collection.
 *
 * <p>Status state machine matches {@link DebitTxnDataService}'s:
 * <pre>
 *    (∅) --insertOrGet--&gt; NEW --markSent--&gt; SENT --markSuccess--&gt; SUCCESS
 *                                             \--markError-----&gt; ERROR
 * </pre>
 *
 * <p>No {@code casUpdateState} — {@code CreditTxn.state} (SETTLED / REFUNDED) is
 * terminal-at-creation and chosen by the factory based on {@code isRefund}.
 *
 * <p>{@link #markSuccess} writes the appropriate field-set based on {@code doc.getState()}:
 * settle-mode analytics ({@code vendorWinAmount, vendorWinLoss, …}) vs refund-mode
 * linkage ({@code referencedDebitGaTxnIds}). Branching lives in the impl, not the contract.
 */
public interface CreditTxnDataService {

    /**
     * Idempotent insert. Returns {@code Optional.empty()} on a fresh successful insert; on
     * doc-id collision returns {@code Optional.of(existing)} so the caller can branch on
     * the persisted {@code status} (load-bearing primitive for Slice 1c idempotency).
     */
    Optional<KvDoc<CreditTxn>> insertOrGet(CreditTxn doc);

    /** KV lookup. {@code null} when not found. */
    KvDoc<CreditTxn> findById(String id);

    /**
     * NEW → SENT. Stamps {@code sentAt}.
     *
     * @param cas when not {@link com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver#NO_CAS},
     *            the write is CAS-guarded and throws
     *            {@link com.couchbase.client.core.error.CasMismatchException} if another caller has
     *            since mutated the document. Used for the {@code ERROR} re-arm, where two concurrent
     *            retries both read {@code ERROR} and must not both reach the operator. A fresh insert
     *            passes {@code NO_CAS} — the atomic insert already established exclusivity.
     */
    void markSent(CreditTxn doc, long cas);

    /**
     * SENT → SUCCESS. Persists {@code vendorBalance} + the state-appropriate field set.
     * Not CAS-guarded: {@code markSent} is the point where exclusivity is established.
     */
    void markSuccess(CreditTxn doc, BigDecimal vendorBalance);

    /**
     * SENT → ERROR. Persists operator/vendor status code in {@code exception}.
     * Not CAS-guarded, and does not check the current status — so it also applies to a
     * {@code NEW} document.
     */
    void markError(CreditTxn doc, String operatorStatusCode);
}
