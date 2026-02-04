package com.nextgen.gameaggregator.core.engine.operator;

import com.nextgen.gameaggregator.core.common.OperatorRequestObject;
import com.nextgen.gameaggregator.core.context.OperatorRequestContext;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;

/**
 * Adapter interface for operator requests.
 * <p>
 * This interface defines two main responsibilities:
 * 1. Validation of the operator API response
 * 2. Mapping exceptions from API or transport layers into domain-level results
 * </p>
 *
 * <p>
 * Implementations of this interface are wallet-action-specific.
 * </p>
 *
 * <p>
 * Key points:
 * - validateClientResponse: Called after a successful API call to check that the response is valid.
 * - handleException: Provides default exception mapping, but can be overridden for custom behavior
 *   such as retries, fallback balances, or vendor-specific error handling.
 * - VendorRequestContext is passed in for exception enrichment, but this creates tight coupling. (TODO: To Be Removed)
 *   Consider using a combined execution context or attaching vendor metadata to the request to remove leakage.
 * </p>
 */
public interface OperatorRequestAdapter {

    /**
     * Validate the client API response.
     */
    void validateClientResponse(ClientApiResponse response, OperatorRequestContext<OperatorRequestObject, OperatorScenario> context, VendorRequestContext vendorContext) throws InvalidOperatorResponseException;

    /**
     * Default exception handler.
     * <p>
     * Maps low-level exceptions into domain-level exceptions or wraps them into OperatorApiException.
     * Can be overridden by concrete adapters to provide specific fallback, retry, or mapping logic.
     * </p>
     */
    default PlayerBalanceData handleException(Exception ex, OperatorApiRequest apiRequest, OperatorRequestContext<OperatorRequestObject, OperatorScenario> context) {
        if (ex instanceof InsufficientBalanceException) {
            throw (InsufficientBalanceException)ex;
        }

        throw new OperatorApiException(ex.getMessage(), ex);
    }
}
