package com.nextgen.gameaggregator.vendor.ezugi.exception;

import org.springframework.stereotype.Component;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.response.ErrorResponse;

@Component
public class EzugiExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.TOKEN_NOT_FOUND));
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.TOKEN_NOT_FOUND));
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.INSUFFICIENT_FUNDS));
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.USER_BLOCKED));
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.USER_BLOCKED));
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.COMPLETED_SUCCESSFULLY, "Transaction already processed"));
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.COMPLETED_SUCCESSFULLY, "Transaction already processed"));
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.GENERAL_ERROR));
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.GENERAL_ERROR));
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("not found")) {
            return new VendorErrorResponse(new ErrorResponse(ResponseCodes.TRANSACTION_NOT_FOUND, "Transaction not found"));
        }
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.TRANSACTION_TIMED_OUT));
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        if (ex.isRoundNotFound() || ex.isBetNotFound()) {
            return new VendorErrorResponse(new ErrorResponse(ResponseCodes.TRANSACTION_NOT_FOUND));
        }
        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.GENERAL_ERROR));
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        Throwable rootCause = getRootCause(ex);

        if (rootCause instanceof BetNotFoundException) {
            if (ex.getMessage() != null && ex.getMessage().contains("RESULT")) {
                return new VendorErrorResponse(
                    new ErrorResponse(ResponseCodes.TRANSACTION_NOT_FOUND, "Credit transaction is still processing")
                );
            }
            return new VendorErrorResponse(new ErrorResponse(ResponseCodes.TRANSACTION_NOT_FOUND));

        } else if (rootCause instanceof com.nextgen.gameaggregator.exception.BetNotFoundException) {
            return new VendorErrorResponse(
                new ErrorResponse(ResponseCodes.TRANSACTION_NOT_FOUND)
            );
        }

        return new VendorErrorResponse(new ErrorResponse(ResponseCodes.GENERAL_ERROR));
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return getRootCause(cause);
        }
        return throwable;
    }
}
