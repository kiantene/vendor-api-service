package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;

/**
 * Adapter interface for operator wallet API requests dispatched via the new
 * debit/credit framework. Sibling of {@link OperatorRequestAdapter} but takes
 * {@link OperatorCallContext} (with {@link OperatorCallTarget}) instead of the
 * legacy {@code OperatorRequestContext} (with {@code GameRound}).
 *
 * <p>Implementations are wallet-action-specific
 * (e.g. {@code CreditOperatorWalletAdapter}, {@code DebitOperatorWalletAdapter}).
 */
public interface OperatorCallAdapter {

    /**
     * Validate the client API response.
     */
    void validateClientResponse(ClientApiResponse response,
                                OperatorCallContext<OperatorRequestObject> context,
                                VendorRequestContext vendorContext)
            throws InvalidOperatorResponseException;

    /**
     * Default exception handler — maps low-level exceptions into domain-level exceptions
     * or wraps them into {@link OperatorApiException}. Override for per-vendor fallback,
     * retry, or mapping logic.
     */
    default PlayerBalanceData handleException(Exception ex,
                                              OperatorApiRequest apiRequest,
                                              OperatorCallContext<OperatorRequestObject> context) {
        if (ex instanceof InsufficientBalanceException ibe) {
            throw ibe;
        }
        throw new OperatorApiException(ex.getMessage(), ex);
    }
}
