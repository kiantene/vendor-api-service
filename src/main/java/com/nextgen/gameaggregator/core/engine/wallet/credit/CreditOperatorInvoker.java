package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.couchbase.client.core.error.CasMismatchException;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.operator.OperatorApiService;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallContext;
import com.nextgen.gameaggregator.core.engine.operator.OperatorCallTarget;
import com.nextgen.gameaggregator.core.engine.operator.wallet.credit.CreditOperatorWalletAdapter;
import com.nextgen.gameaggregator.core.engine.operator.wallet.credit.OperatorCreditRequest;
import com.nextgen.gameaggregator.core.engine.operator.wallet.credit.OperatorCreditRequestMapper;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.couchbase.CreditTxn;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.data.CreditTxnDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Drives a single operator credit call: {@code markSent} → operator execute →
 * vendor view conversion → {@code markSuccess}, or {@code markError} on failure.
 *
 * <p>Uses the new operator-call surface ({@link OperatorCallContext} +
 * {@link OperatorCallTarget}) so the credit pipeline never touches {@code GameRound}.
 *
 * <p>Rethrows the operator exception verbatim so the orchestrator can run its
 * {@code finally} audit block before propagating. A {@code markError} failure is
 * logged and swallowed so it never masks the operator outcome.
 *
 * <p><strong>The operator call is the point of no return.</strong> The {@code try/catch} is bounded
 * to that one call, because {@code markError} means "safe to send again": it puts the doc in the
 * {@code ERROR} re-arm state. Marking a credit the operator already applied would let the next
 * vendor retry re-arm it, claim it via {@code markSent}, and credit the player a second time — and
 * the CAS guard does not help, because a sequential retry presents a matching CAS. So every step
 * after a successful operator call is handled by {@code completeAfterOperatorSuccess}, which never
 * marks ERROR and never reports failure to the vendor once a balance is in hand.
 *
 * <p>{@code markSent} is the point where exclusivity over the txn document is established. On an
 * {@code ERROR} re-arm it is CAS-guarded, and a mismatch means a concurrent retry got there first —
 * surfaced as a duplicate rather than proceeding to a second operator call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditOperatorInvoker {

    private final OperatorApiService operatorApiService;
    private final OperatorCreditRequestMapper operatorCreditRequestMapper;
    private final CreditOperatorWalletAdapter creditOperatorWalletAdapter;
    private final VendorConfigService vendorConfigService;
    private final CreditTxnDataService creditTxnDataService;

    public PlayerBalanceData invoke(CreditContext context, CreditTxn credit, long cas) {
        try {
            creditTxnDataService.markSent(credit, cas);
        } catch (CasMismatchException raced) {
            // Only reachable on an ERROR re-arm. Another retry of this same txn claimed the document
            // first and is calling the operator now; proceeding would double-credit the player.
            throw new DuplicateRequestException(
                    "Credit " + credit.getId() + " was concurrently re-armed by another request");
        }

        OperatorCreditRequest operatorRequest =
                operatorCreditRequestMapper.toOperatorRequest(context, credit);


        OperatorCallTarget target = new OperatorCallTarget(
                context.getAgentId(),
                context.getAgentPlayerUsername(),
                context.getCurrencyCode(),
                context.getToken());

        PlayerBalanceData operatorResult;
        try {
            operatorResult = operatorApiService.execute(
                    creditOperatorWalletAdapter,
                    new OperatorCallContext<>(
                            operatorRequest,
                            vendorConfigService.getTimeoutInMillis(context.getVendorClassName()),
                            EndPoints.WALLET_BET_CREDIT,
                            target),
                    context);
        } catch (RuntimeException ex) {
            // Only the operator call itself may mark this txn ERROR. ERROR is the re-arm state,
            // so it means "safe to send again".
            markErrorBestEffort(credit, ex.getClass().getSimpleName());
            throw ex;
        }

        // Past this line the operator has APPLIED the credit. Nothing here may mark the txn
        // ERROR: a retry would re-arm it, markSent would claim it, and the operator would be
        // called a second time for money that has already moved. The CAS guard on markSent does
        // not help — a sequential retry presents a matching CAS.
        return completeAfterOperatorSuccess(context, credit, operatorResult);
    }

    /**
     * Finishes a credit whose operator call has already succeeded. Never marks the txn ERROR, and
     * never reports failure to the vendor once we have a balance to return — telling a vendor a
     * credit failed after the operator applied it is the reconciliation mismatch this whole
     * pipeline exists to avoid.
     */
    private PlayerBalanceData completeAfterOperatorSuccess(CreditContext context,
                                                          CreditTxn credit,
                                                          PlayerBalanceData operatorResult) {
        // toVendorView is null-safe (TxnAmount coerces a null rate to ZERO rather than throwing),
        // so markSuccess is the only step here that can realistically fail.
        PlayerBalanceData vendorView = operatorResult.toVendorView(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                context.getToVendorRate());

        BigDecimal vendorBalance = vendorView.getBalance();
        credit.setVendorBalance(vendorBalance);
        try {
            creditTxnDataService.markSuccess(credit, vendorBalance);
        } catch (RuntimeException ex) {
            // The credit is applied at the operator; only our bookkeeping failed. Return success
            // so the vendor stops retrying. The doc is left at SENT, deliberately not ERROR:
            // SENT blocks a re-arm (WalletIdempotencyResolver treats it as in-flight), which is
            // what keeps a retry from double-crediting. It needs a reconciler to drain — see the
            // SENT-past-timeout follow-up on OVI-133.
            log.error("Credit {} was applied by the operator but markSuccess failed — doc left SENT "
                            + "for reconciliation, returning success to the vendor: {}",
                    credit.getId(), ex.getMessage(), ex);
        }
        return vendorView;
    }

    private void markErrorBestEffort(CreditTxn credit, String operatorStatusCode) {
        try {
            creditTxnDataService.markError(credit, operatorStatusCode);
        } catch (RuntimeException markErrorEx) {
            log.error("Failed to markError on credit {}: {}",
                    credit.getId(), markErrorEx.getMessage(), markErrorEx);
        }
    }
}
