package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.couchbase.client.core.error.CasMismatchException;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiService;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallTarget;
import com.nextgen.gameaggregator.core.engine.operator.wallet.debit.DebitOperatorWalletAdapter;
import com.nextgen.gameaggregator.core.engine.operator.wallet.debit.OperatorDebitRequest;
import com.nextgen.gameaggregator.core.engine.operator.wallet.debit.OperatorDebitRequestMapper;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.data.DebitTxnDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Drives a single operator debit call: {@code markSent} → operator execute →
 * vendor view conversion → {@code markSuccess}, or {@code markError} on failure.
 *
 * <p>Uses the new operator-call surface ({@link OperatorCallContext} +
 * {@link OperatorCallTarget}) so the debit pipeline never touches {@code GameRound}.
 *
 * <p>Failed debit: {@code markError} + rethrow (debit's "failed bet = no bet"
 * semantic — no ledger append, no kafka downstream).
 *
 * <p><strong>The operator call is the point of no return.</strong> The {@code try/catch} is bounded
 * to that one call; everything after a successful debit runs in
 * {@code completeAfterOperatorSuccess}, which never marks the doc ERROR (a retry would re-arm and
 * debit the player twice) and never reports failure for money that has already moved.
 *
 * <p>{@code markSent} is the point where exclusivity over the txn document is established. On an
 * {@code ERROR} re-arm it is CAS-guarded, and a mismatch means a concurrent retry got there first —
 * surfaced as a duplicate rather than proceeding to a second operator call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebitOperatorInvoker {

    private final OperatorApiService operatorApiService;
    private final OperatorDebitRequestMapper operatorDebitRequestMapper;
    private final DebitOperatorWalletAdapter debitOperatorWalletAdapter;
    private final VendorConfigService vendorConfigService;
    private final DebitTxnDataService debitTxnDataService;

    public PlayerBalanceData invoke(DebitContext context, DebitTxn debit, long cas) {
        try {
            debitTxnDataService.markSent(debit, cas);
        } catch (CasMismatchException raced) {
            // Only reachable on an ERROR re-arm. Another retry of this same txn claimed the document
            // first and is calling the operator now; proceeding would double-debit the player.
            throw new DuplicateRequestException(
                    "Debit " + debit.getId() + " was concurrently re-armed by another request");
        }

        OperatorDebitRequest operatorRequest =
                operatorDebitRequestMapper.toOperatorRequest(context, debit);

        OperatorCallTarget target = new OperatorCallTarget(
                context.getAgentId(),
                context.getAgentPlayerUsername(),
                context.getCurrencyCode(),
                context.getToken());

        PlayerBalanceData operatorResult;
        try {
            operatorResult = operatorApiService.execute(
                    debitOperatorWalletAdapter,
                    new OperatorCallContext<>(
                            operatorRequest,
                            vendorConfigService.getTimeoutInMillis(context.getVendorClassName()),
                            EndPoints.WALLET_BET_DEBIT,
                            target),
                    context);
        } catch (RuntimeException ex) {
            // Only the operator call itself may mark this txn ERROR. ERROR is the re-arm state,
            // so it means "safe to send again".
            markErrorBestEffort(debit, ex.getClass().getSimpleName());
            throw ex;
        }

        // Past this line the operator has APPLIED the debit. Nothing here may mark the txn ERROR
        // (a retry would re-arm and debit the player twice) and nothing may report failure to the
        // vendor for money that has already moved.
        return completeAfterOperatorSuccess(context, debit, operatorResult);
    }

    /**
     * Finishes a debit whose operator call has already succeeded. Mirrors
     * {@link com.nextgen.gameaggregator.core.engine.wallet.credit.CreditOperatorInvoker}'s method
     * of the same name — the two invokers must stay symmetric here, because the failure mode is
     * asymmetric only in which wire code the vendor ends up seeing.
     */
    private PlayerBalanceData completeAfterOperatorSuccess(DebitContext context,
                                                          DebitTxn debit,
                                                          PlayerBalanceData operatorResult) {
        PlayerBalanceData vendorView = operatorResult.toVendorView(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                context.getToVendorRate());

        BigDecimal vendorBalance = vendorView.getBalance();
        debit.setVendorBalance(vendorBalance);
        try {
            debitTxnDataService.markSuccess(debit, vendorBalance);
        } catch (RuntimeException ex) {
            // The debit is applied at the operator; only our bookkeeping failed. Previously this
            // propagated, leaving the doc at SENT — and because WalletIdempotencyResolver treats
            // SENT as in-flight and always throws DuplicateRequestException regardless of
            // returnSuccessOnDuplicate, every JDB Action 9 retry then surfaced as
            // DUPLICATE_TRANSACTION forever with no recovery path. Return success instead so the
            // vendor stops retrying; the SENT doc is a reconciliation item, not a live failure.
            log.error("Debit {} was applied by the operator but markSuccess failed — doc left SENT "
                            + "for reconciliation, returning success to the vendor: {}",
                    debit.getId(), ex.getMessage(), ex);
        }
        return vendorView;
    }

    private void markErrorBestEffort(DebitTxn debit, String operatorStatusCode) {
        try {
            debitTxnDataService.markError(debit, operatorStatusCode);
        } catch (RuntimeException markErrorEx) {
            log.error("Failed to markError on debit {}: {}",
                    debit.getId(), markErrorEx.getMessage(), markErrorEx);
        }
    }
}
