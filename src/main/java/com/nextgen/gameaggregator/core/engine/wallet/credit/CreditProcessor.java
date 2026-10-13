package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Thin orchestrator for the credit pipeline. Delegates each phase to a single-
 * responsibility collaborator:
 *
 * <ol>
 *   <li>{@link CreditPolicy} — pre-flight gates ({@code allowCreditWhenRoundEnded}, the refund
 *       debit-exists check, the {@code DebitReferenceGuard}). Runs BEFORE the claim: it is a pure read-gate, so a
 *       rejected request must not leave a persisted document behind. A request naming a debit that
 *       is already spent is rejected here too — never answered as success.</li>
 *   <li>{@link CreditIdempotencyResolver} — the authoritative {@code insertOrGet} claim; switches on
 *       persisted status (SUCCESS replay / NEW|SENT in-flight throw / ERROR re-arm).</li>
 *   <li>{@link CreditOperatorInvoker} — {@code markSent} → operator call → {@code markSuccess}
 *       (or {@code markError} then rethrow on failure). Builds an
 *       {@link com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext} from the
 *       context directly — no {@code GameRound}.</li>
 *   <li>{@link CreditAuditPublisher} — {@code finally}-block audit-of-attempt: ledger append,
 *       optional {@code markSettled}, transaction-history + bet-history Kafka emits.</li>
 * </ol>
 *
 * <p>The {@code try/finally} structure preserves the legacy invariant: audit side-effects
 * run on BOTH success and failure paths, and never mask the operator exception.
 *
 * <p>For the full request ordering and why each step sits where it does, see the contract on
 * {@link WalletCreditServiceWrapper}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditProcessor {

    private final CreditPolicy creditPolicy;
    private final CreditIdempotencyResolver idempotencyResolver;
    private final CreditOperatorInvoker operatorInvoker;
    private final CreditAuditPublisher auditPublisher;

    /**
     * Optional fast path, called by {@link WalletCreditServiceWrapper} before any session work.
     * Returns the recorded result when this credit has already completed, so a replay never pays
     * for a session lookup that can now fail, never regenerates or mints a session token, and
     * never runs the enrichment chain.
     *
     * <p>Skipping this call costs work, never correctness — {@link #process} repeats the check
     * atomically and remains authoritative on its own.
     *
     * @throws DuplicateRequestException when a concurrent attempt on the same txn id is in flight
     */
    public Optional<PlayerBalanceData> tryReplay(CreditWrapperContext state) {
        CreditContext context = state.getCreditContext();
        return peekFailingOpen(context)
                .map(resolution -> replayResponse(context, state.getConfig(), resolution));
    }

    public PlayerBalanceData process(CreditWrapperContext state) {
        CreditContext context = state.getCreditContext();
        CreditConfig config = state.getConfig();

        try {
            creditPolicy.evaluate(context, config, state.getReferencedDebits());
        } catch (RuntimeException policyRejection) {
            // The gate may have read stale round state: a concurrent attempt on THIS txn id could
            // have completed between the wrapper's peek and here — including the very settle that
            // closed the round. A recorded SUCCESS outranks the gate, so re-peek. This can only
            // ever UPGRADE the outcome; any other peek result leaves the gate's verdict standing.
            Optional<WalletIdempotencyResolver.Resolution<CreditTxn>> recorded = peekQuietly(context);
            if (recorded.isPresent()) {
                return replayResponse(context, config, recorded.get());
            }
            // Replaces the ERROR doc the old ordering wrote and immediately re-armed: name the
            // rejected txn in the wallet-doc namespace, since no document is persisted for it.
            log.warn("Credit {}::{} rejected by policy ({}) — no txn doc persisted",
                    context.getVendorClassName(), context.getVendorTxnId(),
                    policyRejection.getClass().getSimpleName());
            throw policyRejection;
        }

        WalletIdempotencyResolver.Resolution<CreditTxn> resolution =
                idempotencyResolver.resolve(context, System.currentTimeMillis());
        if (resolution.isReplay()) {
            return replayResponse(context, config, resolution);
        }

        CreditTxn credit = resolution.getTxn();

        PlayerBalanceData vendorView = null;
        RuntimeException captured = null;
        boolean operatorSucceeded = false;

        try {
            // resolution.getCas() is non-zero only for an ERROR re-arm, where it guards markSent
            // so two concurrent retries of the same failed txn cannot both reach the operator.
            vendorView = operatorInvoker.invoke(
                    context, credit, resolution.getCas());
            operatorSucceeded = true;
        } catch (RuntimeException ex) {
            captured = ex;
        } finally {
            auditPublisher.publish(
                    context,
                    credit,
                    config,
                    vendorView != null ? vendorView.getBalance() : null,
                    operatorSucceeded);
        }

        if (captured != null) {
            throw captured;
        }
        return vendorView;
    }

    /**
     * Peek for {@link #tryReplay}. An in-flight collision must still stop the request before any
     * session work, so {@link DuplicateRequestException} propagates. Anything else — a KV timeout,
     * a bucket not ready — falls through to the full pipeline: the peek is an optimisation, and the
     * claim is authoritative, so failing open costs nothing. Failing closed would turn an otherwise
     * fine request into a vendor-visible error at the very front of the pipeline.
     */
    private Optional<WalletIdempotencyResolver.Resolution<CreditTxn>> peekFailingOpen(CreditContext context) {
        try {
            return idempotencyResolver.peek(context);
        } catch (DuplicateRequestException inFlight) {
            throw inFlight;
        } catch (RuntimeException ex) {
            log.warn("Credit replay peek failed for {}::{} — falling through to the full pipeline: {}",
                    context.getVendorClassName(), context.getVendorTxnId(), ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Peek for the policy-rejection path, where the only useful answer is "this actually already
     * succeeded". Swallows everything else, in-flight collisions included: a concurrent sibling
     * does not change the gate's verdict, and the re-peek must never be able to substitute a new
     * error for the policy rejection the vendor should see.
     */
    private Optional<WalletIdempotencyResolver.Resolution<CreditTxn>> peekQuietly(CreditContext context) {
        try {
            return idempotencyResolver.peek(context);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * The single home for what a replay looks like on the wire: per-vendor config decides between
     * silent dedupe (prior balance as success) and the vendor's {@code DUPLICATE_TRANSACTION} code.
     * Shared by the wrapper's fast path, the claim, and the policy-rejection re-peek so all three
     * answer identically.
     */
    private PlayerBalanceData replayResponse(CreditContext context,
                                            CreditConfig config,
                                            WalletIdempotencyResolver.Resolution<CreditTxn> resolution) {
        if (config.isReturnSuccessOnDuplicate()) {
            return buildVendorView(context, resolution.getReplayBalance());
        }
        throw new DuplicateRequestException(
                "Credit " + context.getVendorClassName() + "::" + context.getVendorTxnId() + " already settled");
    }

    /**
     * The recorded {@code vendorBalance} was already converted to vendor view before {@code
     * markSuccess} persisted it, so flag it as such — a later {@code toVendorView} must be a no-op
     * rather than a second rate multiplication.
     */
    private PlayerBalanceData buildVendorView(CreditContext context, BigDecimal vendorBalance) {
        return PlayerBalanceData.ofVendorView(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                vendorBalance);
    }
}
