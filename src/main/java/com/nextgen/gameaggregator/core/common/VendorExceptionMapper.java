package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.exception.InternalConfigurationException;
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
    VendorErrorResponse onDuplicateBet(DuplicateBetException ex);

    /**
     * These are unexpected errors
     */
    VendorErrorResponse onInvalidRequestError(InvalidRequestException ex); // Thrown by RequestValidationExceptionHandler from @Valid in controller
    VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex);
    VendorErrorResponse onInternalError(InternalServerException ex);
}
