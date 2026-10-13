package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.couchbase.WalletRoundLedger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Thin orchestrator for the debit pipeline. Delegates each phase to a single-
 * responsibility collaborator:
 *
 * <ol>
 *   <li>{@link DebitIdempotencyResolver} — the authoritative {@code insertOrGet} claim
 *       (SUCCESS replay / NEW|SENT in-flight throw / ERROR re-arm).</li>
 *   <li>{@link DebitLedgerInitializer} — {@code WalletRoundLedger} insertOrGet
 *       (idempotent across subsequent legs of the same round).</li>
 *   <li>{@link DebitOperatorInvoker} — {@code markSent} → operator call →
 *       {@code markSuccess} (or {@code markError} + rethrow on failure). Builds an
 *       {@link com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext} from the
 *       context directly — no {@code GameRound}.</li>
 *   <li>{@link DebitAuditPublisher} — success-only audit:
 *       {@code appendTxnRef} + transaction-history Kafka emit. Failed debit writes nothing —
 *       preserves the legacy "failed bet = no bet" semantic.</li>
 * </ol>
 *
 * <p>There is no policy phase on the debit path, so unlike {@link
 * com.nextgen.gameaggregator.core.engine.wallet.credit.CreditProcessor} nothing here had to be
 * reordered: {@link #process} was already self-sufficient, and {@link #tryReplay} is a pure
 * optimisation.
 *
 * <p>For the full request ordering see the contract on {@link WalletDebitServiceWrapper}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebitProcessor {

    private final DebitIdempotencyResolver idempotencyResolver;
    private final DebitLedgerInitializer ledgerInitializer;
    private final DebitOperatorInvoker operatorInvoker;
    private final DebitAuditPublisher auditPublisher;

    /**
     * Optional fast path, called by {@link WalletDebitServiceWrapper} before any session work.
     * Returns the recorded result when this debit has already completed, so a replay never pays for
     * a session lookup that can now fail and never runs the enrichment chain.
     *
     * <p>Skipping this call costs work, never correctness — {@link #process} repeats the check
     * atomically and remains authoritative on its own.
     *
     * @throws DuplicateRequestException when a concurrent attempt on the same txn id is in flight
     */
    public Optional<PlayerBalanceData> tryReplay(DebitWrapperContext state) {
        DebitContext context = state.getDebitContext();
        return peekFailingOpen(context)
                .map(resolution -> replayResponse(context, state.getConfig(), resolution));
    }

    public PlayerBalanceData process(DebitWrapperContext state) {
        DebitContext context = state.getDebitContext();
        DebitConfig config = state.getConfig();
        long now = System.currentTimeMillis();

        WalletIdempotencyResolver.Resolution<DebitTxn> resolution = idempotencyResolver.resolve(context, now);
        if (resolution.isReplay()) {
            return replayResponse(context, config, resolution);
        }

        DebitTxn debit = resolution.getTxn();
        KvDoc<WalletRoundLedger> ledgerKv = ledgerInitializer.ensure(context, now);

        // resolution.getCas() is non-zero only for an ERROR re-arm, where it guards markSent so two
        // concurrent retries of the same failed txn cannot both reach the operator.
        PlayerBalanceData vendorView = operatorInvoker.invoke(context, debit, resolution.getCas());

        auditPublisher.publishSuccess(context, debit, vendorView.getBalance(), ledgerKv);
        return vendorView;
    }

    /**
     * An in-flight collision must still stop the request before any session work, so
     * {@link DuplicateRequestException} propagates. Anything else — a KV timeout, a bucket not
     * ready — falls through to the full pipeline: the peek is an optimisation, and the claim is
     * authoritative, so failing open costs nothing. Failing closed would turn an otherwise fine
     * request into a vendor-visible error at the very front of the pipeline.
     */
    private Optional<WalletIdempotencyResolver.Resolution<DebitTxn>> peekFailingOpen(DebitContext context) {
        try {
            return idempotencyResolver.peek(context);
        } catch (DuplicateRequestException inFlight) {
            throw inFlight;
        } catch (RuntimeException ex) {
            log.warn("Debit replay peek failed for {}::{} — falling through to the full pipeline: {}",
                    context.getVendorClassName(), context.getVendorTxnId(), ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * The single home for what a replay looks like on the wire: per-vendor config decides between
     * silent dedupe (prior balance as success) and the vendor's {@code DUPLICATE_TRANSACTION} code.
     * Shared by the wrapper's fast path and the claim so both answer identically.
     */
    private PlayerBalanceData replayResponse(DebitContext context,
                                            DebitConfig config,
                                            WalletIdempotencyResolver.Resolution<DebitTxn> resolution) {
        if (config.isReturnSuccessOnDuplicate()) {
            return buildVendorView(context, resolution.getReplayBalance());
        }
        throw new DuplicateRequestException(
                "Debit " + context.getVendorClassName() + "::" + context.getVendorTxnId() + " already settled");
    }

    /**
     * The recorded {@code vendorBalance} was already converted to vendor view before {@code
     * markSuccess} persisted it, so flag it as such — a later {@code toVendorView} must be a no-op
     * rather than a second rate multiplication.
     */
    private PlayerBalanceData buildVendorView(DebitContext context, BigDecimal vendorBalance) {
        return PlayerBalanceData.ofVendorView(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                vendorBalance);
    }
}
