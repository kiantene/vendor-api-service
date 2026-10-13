package com.nextgen.gameaggregator.core.engine.wallet.idempotency;

import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletTxn;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Shared base for the debit/credit pipelines' idempotency check. Closes the get-then-save race
 * window present in the legacy {@code DuplicateRequestGuard} by relying on a single atomic
 * Couchbase KV primitive ({@code insertOrGet}).
 *
 * <h3>Two entry points: {@link #peek} then {@link #resolve}</h3>
 *
 * The check is deliberately split, because the two halves have different data dependencies:
 *
 * <table border="1">
 *   <caption>peek vs resolve</caption>
 *   <tr><th></th><th>{@link #peek}</th><th>{@link #resolve}</th></tr>
 *   <tr><td>Reads</td>
 *       <td><b>wire fields only</b> — {@link #docId(Object)} is built from {@code vendorClassName}
 *           + {@code vendorTxnId}</td>
 *       <td>the <b>enriched</b> context — {@link #buildFreshTxn} needs {@code vendorId},
 *           {@code gameCode}, {@code currencyCode}</td></tr>
 *   <tr><td>Writes</td><td>never — a plain KV get</td><td>yes — atomic {@code insertOrGet}</td></tr>
 *   <tr><td>Can run</td><td>before session lookup and enrichment</td><td>only after enrichment</td></tr>
 *   <tr><td>Can return {@code proceed}</td><td><b>no</b> — replay or nothing</td><td>yes</td></tr>
 *   <tr><td>Authority</td><td>advisory fast path</td><td><b>authoritative</b></td></tr>
 * </table>
 *
 * <p>Both share one verdict function ({@code classify}), so the two paths cannot disagree about
 * what a given persisted document means.
 *
 * <p>Status outcomes, identical for both entry points:
 * <ul>
 *   <li>persisted {@code SUCCESS} → {@link Resolution#replay(BigDecimal)}; the caller's per-vendor
 *       config decides whether to return the prior balance (silent dedupe) or throw
 *       {@link DuplicateRequestException} so the exception mapper emits the vendor's
 *       {@code DUPLICATE_TRANSACTION} wire code.</li>
 *   <li>persisted {@code NEW} or {@code SENT} → {@link DuplicateRequestException}; caller stops.
 *       In-flight collisions are always surfaced regardless of vendor config — no legitimate vendor
 *       wants silent dedupe on a not-yet-completed call.</li>
 *   <li>persisted {@code ERROR} → re-arm. {@link #resolve} proceeds with the <b>persisted</b> doc
 *       (same {@code gaTxnId}); {@link #peek} returns empty so the caller falls through to the claim.</li>
 *   <li>absent → {@link #resolve} proceeds with the fresh doc; {@link #peek} returns empty.</li>
 * </ul>
 *
 * <p>For where these sit in the request pipeline, and why, see
 * {@link com.nextgen.gameaggregator.core.engine.wallet.credit.WalletCreditServiceWrapper}.
 *
 * <p>Subclasses provide the doc-type-specific factory, doc id, and persistence primitives.
 *
 * @param <C> context type (e.g. {@code CreditContext}, {@code DebitContext})
 * @param <T> txn doc type (must be a {@link WalletTxn})
 */
@Slf4j
public abstract class WalletIdempotencyResolver<C, T extends WalletTxn> {

    /**
     * Advisory pre-claim check — a key-only KV get, never a write.
     *
     * <p>Uses <b>only wire-supplied fields</b> (via {@link #docId(Object)}), so this may run before
     * session lookup and enrichment. That is the whole point: a request that has already completed
     * must not pay for a session lookup that can now fail, must not regenerate or mint a session
     * token, and must not run the enrichment chain.
     *
     * <p>Never returns {@link Resolution#proceed}: a present result always means "stop, this is a
     * replay". Absent means "nothing decided here" — the caller continues to {@link #resolve},
     * which performs the authoritative atomic claim.
     *
     * @return a replay {@link Resolution} when a terminal {@code SUCCESS} doc already exists;
     *         {@link Optional#empty()} when the doc is absent or in {@code ERROR}
     * @throws DuplicateRequestException when the doc is {@code NEW}/{@code SENT} (in-flight collision)
     */
    public Optional<Resolution<T>> peek(C context) {
        KvDoc<T> existing = findById(docId(context));
        if (existing == null) {
            return Optional.empty();
        }
        return classify(existing.getPayload());
    }

    /**
     * Authoritative idempotency claim. Atomically inserts the fresh doc or reads back whatever is
     * already persisted, then applies the same verdict as {@link #peek}.
     *
     * <p>Must remain correct when {@link #peek} was never called — {@code peek} is an optimisation,
     * not a prerequisite.
     */
    public Resolution<T> resolve(C context, long now) {
        T fresh = buildFreshTxn(context, now);
        Optional<KvDoc<T>> existing = insertOrGet(fresh);
        if (existing.isEmpty()) {
            return Resolution.proceed(fresh);
        }
        T persisted = existing.get().getPayload();
        long cas = existing.get().getCas();
        // ERROR / unknown → re-arm with the PERSISTED doc so the retry reuses the same gaTxnId.
        // Never the fresh one, whose gaTxnId was just generated and never stored. The CAS travels
        // with it so the caller's markSent can claim the re-arm exclusively — see Resolution#getCas.
        return classify(persisted).orElseGet(() -> Resolution.proceed(persisted, cas));
    }

    /**
     * The single terminal-status verdict, shared by {@link #peek} and {@link #resolve} so the fast
     * path and the authoritative path can never disagree about the same document.
     *
     * @return the replay resolution for {@code SUCCESS}; empty for {@code ERROR} / unknown
     *         ("not terminal — caller decides")
     * @throws DuplicateRequestException for {@code NEW} / {@code SENT}
     */
    private Optional<Resolution<T>> classify(T persisted) {
        switch (persisted.getStatus()) {
            case SUCCESS:
                log.info("{} {} replay — returning prior vendorBalance={}",
                        entityKind(), persisted.getId(), persisted.getVendorBalance());
                return Optional.of(Resolution.replay(persisted.getVendorBalance()));
            case NEW:
            case SENT:
                // In-flight: NEW means a concurrent caller just inserted and is about to
                // markSent + call the operator. SENT means the operator call is already
                // in progress. Treating both as duplicates closes the race window between
                // insertOrGet and markSent.
                throw new DuplicateRequestException(
                        entityKind() + " " + persisted.getId() + " is still being processed");
            case ERROR:
            default:
                return Optional.empty();
        }
    }

    /** Build a fresh {@link WalletTxn} (status=NEW) populated with audit fields from {@code context}. */
    protected abstract T buildFreshTxn(C context, long now);

    /** Atomic {@code insertOrGet} on the doc-type-specific data service. */
    protected abstract Optional<KvDoc<T>> insertOrGet(T fresh);

    /**
     * Couchbase doc id for this request.
     *
     * <p><b>Must read wire-supplied fields only</b> ({@code vendorClassName} + {@code vendorTxnId}).
     * Reaching for an enriched or session-derived field here would break {@link #peek}, which runs
     * before enrichment, and would make the peek key differ from the claim key.
     */
    protected abstract String docId(C context);

    /** Key-only KV get. {@code null} when the doc is absent. */
    protected abstract KvDoc<T> findById(String docId);

    /** Short label ("Credit" / "Debit") used in log + exception messages. */
    protected abstract String entityKind();

    public static final class Resolution<T extends WalletTxn> {
        private final T txn;
        private final BigDecimal replayBalance;
        /**
         * Explicit discriminator. NOT inferred from {@code replayBalance != null}: {@code markSuccess}
         * persists {@code vendorBalance} only when non-null, so a {@code SUCCESS} doc can legitimately
         * carry a null balance. Inferring would silently downgrade that to "proceed" with a null
         * {@code txn} — an NPE at {@code markSent}, or a second operator call for an already-successful
         * transaction.
         */
        private final boolean replay;
        /**
         * Couchbase CAS of the persisted document this resolution proceeds with, or {@code 0} when
         * there is none to guard against — see {@link #getCas()}.
         */
        private final long cas;

        private Resolution(T txn, BigDecimal replayBalance, boolean replay, long cas) {
            this.txn = txn;
            this.replayBalance = replayBalance;
            this.replay = replay;
            this.cas = cas;
        }

        /**
         * Fresh insert. No CAS is needed: this caller just won the atomic {@code insert}, so no
         * other thread holds the document.
         */
        public static <T extends WalletTxn> Resolution<T> proceed(T txn) {
            return new Resolution<>(txn, null, false, NO_CAS);
        }

        /** Re-arm of a persisted {@code ERROR} document, carrying its CAS. */
        public static <T extends WalletTxn> Resolution<T> proceed(T txn, long cas) {
            return new Resolution<>(txn, null, false, cas);
        }

        public static <T extends WalletTxn> Resolution<T> replay(BigDecimal vendorBalance) {
            return new Resolution<>(null, vendorBalance, true, NO_CAS);
        }

        public boolean isReplay() {
            return replay;
        }

        public T getTxn() {
            return txn;
        }

        public BigDecimal getReplayBalance() {
            return replayBalance;
        }

        /**
         * CAS to guard the caller's {@code markSent} with, or {@link #NO_CAS} for an unguarded write.
         *
         * <p>Only the {@code ERROR} re-arm races: two concurrent vendor retries of the same failed
         * txn both read {@code ERROR} and both proceed, so without a CAS both would {@code markSent}
         * and both would call the operator — a double credit. A CAS-guarded {@code markSent} lets
         * exactly one win; the loser sees a mismatch and must abort as a duplicate.
         *
         * <p>The fresh-insert path genuinely needs no guard, which is why this is {@link #NO_CAS}
         * there: the atomic {@code insert} already established exclusivity. That also avoids
         * disturbing {@code insertOrGet}'s "empty means fresh insert" contract, which the whole
         * status switch depends on.
         */
        public long getCas() {
            return cas;
        }
    }

    /** Sentinel for "no CAS to guard against" — an unguarded write. */
    public static final long NO_CAS = 0L;
}
