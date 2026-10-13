package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetAlreadySettledException;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.BetResultRejectedException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.RoundAlreadyRefundedException;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * The one owner of "which debits does this credit close": loading them, checking a credit may
 * close them, and marking them closed once it has. What stops the same bet being paid twice.
 *
 * <p>Runs for every vendor whenever a credit names debits in
 * {@code referencedVendorDebitTxnIds}: a named debit is always checked and always marked. The only
 * per-vendor choice is whether naming them is mandatory ({@link CreditConfig#isRequireDebitReference}).
 * A vendor whose protocol has no debit reference simply leaves the field empty.
 *
 * <p>A credit has two identities: the request ({@code vendorTxnId}) and the resource it consumes
 * (the referenced {@link DebitTxn}). {@code WalletIdempotencyResolver} gives exclusivity on the
 * request only, so a second credit or cancel bearing a fresh transfer id is, by that key, a
 * brand-new request. This class keys on the resource (OVI-2167 / 3185 / 3187 / 3188).
 *
 * <p>Called by {@link CreditPolicy} before the claim ({@link #requirePresent}, {@link #load},
 * {@link #check}) and by {@link CreditAuditPublisher} after the operator call
 * ({@link #markConsumed}). Keeping both ends here is what lets the check and the mark agree on
 * what "consumed" means.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebitReferenceGuard {

    private final DebitTxnDataService debitTxnDataService;

    /**
     * When the vendor requires a debit reference, a credit that names no debit is malformed. A
     * request-shape error, so it runs before any state check and maps to the vendor's
     * invalid-request code.
     */
    public void requirePresent(CreditContext context, boolean required) {
        if (!required) {
            return;
        }
        List<String> refs = context.getReferencedVendorDebitTxnIds();
        if (refs == null || refs.isEmpty()) {
            throw new InvalidRequestException(
                    (context.isRefund() ? "Refund" : "Settle") + " must reference the debits it closes");
        }
    }

    /**
     * Loads each {@code referencedVendorDebitTxnIds} entry once, preserving request order and
     * keeping {@code null} for a doc that is absent, so callers can tell "missing" from "present
     * but wrong". Empty when the request names none.
     */
    public List<KvDoc<DebitTxn>> load(CreditContext context) {
        List<String> refs = context.getReferencedVendorDebitTxnIds();
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        List<KvDoc<DebitTxn>> loaded = new ArrayList<>(refs.size());
        for (String vendorDebitTxnId : refs) {
            loaded.add(debitTxnDataService.findById(
                    DebitTxn.createDocId(context.getVendorClassName(), vendorDebitTxnId)));
        }
        return loaded;
    }

    /**
     * Binds the credit to the debits it names. Three checks, in order:
     *
     * <ol>
     *   <li><b>Already consumed</b> — any referenced debit whose {@code state} has left
     *       {@code UNSETTLED} was closed by an earlier credit. This request, under a fresh
     *       {@code vendorTxnId}, would move no money, so it is <b>rejected</b> rather than answered
     *       as success: success means the credit was applied (OVI-2167 / 3185 / 3187).
     *     <ul>
     *       <li>refund → {@link RollbackNotAllowedException} (JDB {@code 6101 Can not cancel});</li>
     *       <li>settle → {@link BetResultRejectedException} (JDB {@code 9999}).</li>
     *     </ul>
     *     The cause says how the debit was closed: {@link BetAlreadySettledException} if any
     *     referenced debit is SETTLED, else {@link RoundAlreadyRefundedException}.</li>
     *   <li><b>Resolvable</b> — missing or not {@code SUCCESS} → {@link BetNotFoundException}.</li>
     *   <li><b>Round-bound</b> — a debit belonging to a different round than this request's →
     *       {@link BetNotFoundException} (OVI-3188).</li>
     * </ol>
     *
     * <p>A single consumed entry rejects the whole request even when its siblings are unconsumed:
     * paying out the remainder would make a half-applied credit impossible to reconcile against
     * the vendor's own view. It is logged at WARN because it is not a shape JDB should ever send.
     *
     * <p>A replay of the credit that consumed the debit carries the <i>same</i> {@code vendorTxnId}
     * and never gets here as a success: the wrapper's peek answers it first, and if a race slips it
     * past the peek, {@code CreditProcessor} re-peeks on this rejection and returns the recorded
     * result instead.
     */
    public void check(CreditContext context, List<KvDoc<DebitTxn>> referenced) {
        List<String> refs = context.getReferencedVendorDebitTxnIds();
        if (refs == null || refs.isEmpty()) {
            return;
        }

        DebitTxn firstConsumed = null;
        DebitTxn firstSettled = null;
        int consumedCount = 0;
        for (KvDoc<DebitTxn> debitKv : referenced) {
            if (debitKv != null && !debitKv.getPayload().isUnsettled()) {
                consumedCount++;
                if (firstConsumed == null) {
                    firstConsumed = debitKv.getPayload();
                }
                if (firstSettled == null && GameRoundState.SETTLED == debitKv.getPayload().getState()) {
                    firstSettled = debitKv.getPayload();
                }
            }
        }
        if (firstConsumed != null) {
            DebitTxn reported = firstSettled != null ? firstSettled : firstConsumed;
            if (consumedCount < refs.size()) {
                // Partial overlap: not a shape JDB should ever send. Louder than a plain repeat,
                // because it means the vendor mixed a spent debit in with live ones.
                log.warn("Credit {}::{} references {} debits, {} of them already consumed "
                                + "(first: {} = {}) — rejecting the whole request",
                        context.getVendorClassName(), context.getVendorTxnId(), refs.size(),
                        consumedCount, reported.getId(), reported.getState());
            }
            String message = (context.isRefund() ? "Refund" : "Settle") + " rejected: referenced debit "
                    + reported.getId() + " is already " + reported.getState();
            RuntimeException cause = firstSettled != null
                    ? new BetAlreadySettledException(message)
                    : new RoundAlreadyRefundedException(message);
            throw context.isRefund()
                    ? new RollbackNotAllowedException(context, cause)
                    : new BetResultRejectedException(context, cause, null);
        }

        for (int i = 0; i < refs.size(); i++) {
            KvDoc<DebitTxn> debitKv = referenced.get(i);
            String debitDocId = DebitTxn.createDocId(context.getVendorClassName(), refs.get(i));
            if (debitKv == null || !debitKv.getPayload().isSuccess()) {
                throw new BetNotFoundException(
                        "Credit rejected: referenced debit " + debitDocId + " missing or not SUCCESS");
            }
            String debitRoundId = debitKv.getPayload().getVendorRoundId();
            if (debitRoundId == null || !debitRoundId.equals(context.getVendorRoundId())) {
                throw new BetNotFoundException("Credit rejected: referenced debit " + debitDocId
                        + " belongs to round " + debitRoundId
                        + ", not the requested round " + context.getVendorRoundId());
            }
        }
    }

    /**
     * Flips each referenced {@link DebitTxn} out of {@code UNSETTLED}, so a later credit or cancel
     * naming the same debit is recognised as a repeat by {@link #check}. {@code SETTLED} for
     * a settle, {@code REFUNDED} for a refund.
     *
     * <p><b>After the operator call, deliberately.</b> Claiming the debit first would make the CAS
     * flip an atomic claim and close the window where two credits with different transfer ids race
     * for the same debit — but an operator failure would then leave the debit marked with no money
     * moved, and the vendor's retry would be rejected as already consumed: a lost payout. Marking only on
     * success keeps the failure mode "the retry works normally". The residual race is narrower than
     * what it replaces and needs an attribution field on the debit to close properly.
     *
     * <p>The doc is re-read here rather than reusing the CAS {@link #check} saw, because
     * {@code casUpdateState} retries with the caller's CAS unchanged and so cannot converge on a
     * stale one.
     *
     * <p>Best-effort with its own try/catch, like every other step: the operator has already
     * applied this credit, so a marker failure must not fail the vendor response. It is logged at
     * ERROR because losing it re-opens the double-spend for that debit.
     */
    public void markConsumed(CreditContext context) {
        List<String> refs = context.getReferencedVendorDebitTxnIds();
        if (refs == null || refs.isEmpty()) {
            return;
        }
        GameRoundState consumedState = context.isRefund()
                ? GameRoundState.REFUNDED
                : GameRoundState.SETTLED;
        for (String vendorDebitTxnId : refs) {
            String debitDocId = DebitTxn.createDocId(context.getVendorClassName(), vendorDebitTxnId);
            try {
                KvDoc<DebitTxn> debitKv = debitTxnDataService.findById(debitDocId);
                if (debitKv == null) {
                    log.error("Cannot mark debit {} as {} for credit {}::{} — document not found",
                            debitDocId, consumedState, context.getVendorClassName(),
                            context.getVendorTxnId());
                    continue;
                }
                debitTxnDataService.casUpdateState(debitKv, consumedState);
            } catch (RuntimeException ex) {
                log.error("Failed to mark debit {} as {} for credit {}::{} — the debit stays "
                                + "spendable and may be credited again: {}",
                        debitDocId, consumedState, context.getVendorClassName(),
                        context.getVendorTxnId(), ex.getMessage(), ex);
            }
        }
    }
}
