package com.nextgen.gameaggregator.vendor.endorphina.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.vendor.endorphina.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.endorphina.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.endorphina.response.ErrorResponse;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class EndorphinaExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.TOKEN_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.TOKEN_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.INSUFFICIENT_FUNDS);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.TOKEN_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        // BetController & BetResultController already has returnSuccessOnDuplicate set to true
        // In normal case, this exception should never be thrown, unless there is a bug
        // therefore we map to internal server exception
        return onInternalError(new InternalServerException(ex.getMessage(), ex));
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(InvalidPlayerException.class)) {
            return getErrorResponse(ResponseCodes.TOKEN_NOT_FOUND);
        }
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes responseCodes) {
        return new VendorErrorResponse(new ErrorResponse(responseCodes));
    }
}
