package com.nextgen.gameaggregator.core.exception.mapper;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.vendor.VendorComponent;

public interface VendorExceptionMapper extends VendorComponent {

    /**
     * These are business scenarios
     */
    VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex);
    VendorErrorResponse onGameTerminated(GameTerminatedException ex);
    VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex);
    VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex);
    VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex);
    VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex);
    default VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return onDuplicateRequest(new DuplicateRequestException(ex.getMessage(), ex));
    }
    default VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return onInternalError(new InternalServerException(ex.getMessage(), ex));
    }
    default VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return onInternalError(new InternalServerException("isAllowRollbackForSettledBet is configured to false", ex));
    }

    /**
     * These are unexpected errors
     */
    VendorErrorResponse onInvalidRequestError(InvalidRequestException ex); // Thrown by RequestValidationExceptionHandler from @Valid in controller
    default VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return onInternalError(new InternalServerException(ex.getMessage(), ex));
    }
    VendorErrorResponse onInternalError(InternalServerException ex);
}
