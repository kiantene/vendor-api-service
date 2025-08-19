package com.nextgen.gameaggregator.core.exception.mapper;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.exception.*;

public interface VendorExceptionMapper {
    String getVendorClassName();

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

    /**
     * These are unexpected errors
     */
    VendorErrorResponse onInvalidRequestError(InvalidRequestException ex); // Thrown by RequestValidationExceptionHandler from @Valid in controller
    default VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return onInternalError(new InternalServerException(ex.getMessage(), ex));
    }
    VendorErrorResponse onInternalError(InternalServerException ex);
}
