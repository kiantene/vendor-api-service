package com.nextgen.gameaggregator.service.data;

import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Persistence facade for {@link DebitTxn} standalone Couchbase documents in the
 * {@code wallet_round_transactions} collection.
 *
 * <p>Status state machine:
 * <pre>
 *    (∅) --insertOrGet--&gt; NEW --markSent--&gt; SENT --markSuccess--&gt; SUCCESS
 *                                             \--markError-----&gt; ERROR
 * </pre>
 *
 * <p>Reference-state lifecycle ({@code DebitTxn.state}) is independent and CAS-updated
 * by {@link #casUpdateState} when a successful {@code CreditTxn} references this debit
 * (wired up in Step 2 follow-up).
 *
 * <p>Mirrors the shape of {@link GameTransactionDataService} but typed for {@link DebitTxn}.
 */
public interface DebitTxnDataService {

    /**
     * Idempotent insert. Returns {@code Optional.empty()} on a fresh successful insert; on
     * doc-id collision returns {@code Optional.of(existing)} so the caller can branch on
     * the persisted {@code status} (load-bearing primitive for Slice 1c idempotency).
     */
    Optional<KvDoc<DebitTxn>> insertOrGet(DebitTxn doc);

    /** KV lookup. {@code null} when not found (matches {@link GameRoundDataService#findById}). */
    KvDoc<DebitTxn> findById(String id);

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
    void markSent(DebitTxn doc, long cas);

    /**
     * SENT → SUCCESS. Persists {@code vendorBalance} (vendor-facing, post-FX). Stamps {@code doneAt}.
     * Not CAS-guarded: {@code markSent} is the point where exclusivity is established.
     */
    void markSuccess(DebitTxn doc, BigDecimal vendorBalance);

    /**
     * SENT → ERROR. Persists operator/vendor status code in {@code exception}. Stamps {@code doneAt}.
     * Not CAS-guarded, and does not check the current status — so it also applies to a
     * {@code NEW} document.
     */
    void markError(DebitTxn doc, String operatorStatusCode);

    /**
     * CAS-update {@code state} on the standalone doc. Used by a successful credit txn
     * to flip an upstream debit from {@code UNSETTLED} to {@code SETTLED} / {@code REFUNDED}.
     * Wired in Step 2 follow-up.
     */
    void casUpdateState(KvDoc<DebitTxn> kvDoc, GameRoundState newState);
}
