package com.nextgen.gameaggregator.vendor.crystal.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.crystal.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.crystal.response.ErrorResponse;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class CrystalExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.INSUFFICIENT_FUNDS);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCodes.TXN_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
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
        return getErrorResponse(ResponseCodes.INVALID_PARAMETERS);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(BetNotFoundException.class)) {
            return getErrorResponse(ResponseCodes.TXN_NOT_FOUND);
        }
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.TXN_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.TXN_NOT_FOUND);
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes responseCodes) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .data(null)
                .error(ErrorResponse.Error.of(responseCodes))
                .build();

        return new VendorErrorResponse(responseCodes.getHttpStatus(), errorResponse);
    }
}
