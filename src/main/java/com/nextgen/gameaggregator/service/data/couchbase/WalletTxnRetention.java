package com.nextgen.gameaggregator.service.data.couchbase;

import java.time.Duration;

/**
 * How long a wallet txn document survives once it reaches a terminal state.
 *
 * <p>The clock starts at terminal, never at creation — mirroring the legacy
 * {@code CouchbaseGameTransactionDataService.updateToSettled / updateToRefunded}. An
 * <b>unsettled</b> {@code DebitTxn} or an errored {@code CreditTxn} is an open liability: money has
 * moved with nothing closing it, which is exactly what unsettled-bet reconciliation exists to find.
 * Those must never expire, so no TTL is applied on {@code markSuccess} for a debit, on
 * {@code markError}, or on {@code markSent}.
 *
 * <h3>Known limitation — this value is below the idempotency floor</h3>
 *
 * These documents are not only audit; they are the idempotency record. Once one expires:
 *
 * <ul>
 *   <li>a vendor retry of the same transaction finds nothing, {@code insertOrGet} inserts fresh,
 *       and the operator is called a second time;</li>
 *   <li>the fresh document mints a new {@code gaTxnId}, which is what
 *       {@code OperatorCreditRequestMapper} sends as the operator's {@code transactionId} — so the
 *       operator's own idempotency cannot catch it either;</li>
 *   <li>for a {@code DebitTxn} specifically, the consumed marker behind OVI-2167 / 3185 / 3187 is
 *       lost, so a late cancel is answered {@code 9999} instead of the {@code 0000} no-op.</li>
 * </ul>
 *
 * <p>JDB retries cancel for up to 12h and the retry queue runs to ~12.3h, so 3 hours sits below
 * every window that matters. It is set here deliberately, for storage cost, matching the legacy
 * value. Raising it to at least 72h is the correctness-driven value, and is expected to follow the
 * deterministic-{@code transactionId} work (OVI-3190), which makes the operator's dedupe a real
 * backstop rather than one GA defeats by minting a fresh id per attempt.
 */
public final class WalletTxnRetention {

    /** Matches the legacy {@code GameTransaction} / {@code GameRound} terminal TTL. */
    public static final Duration TERMINAL_TTL = Duration.ofHours(3);

    private WalletTxnRetention() {
    }
}
