package com.nextgen.gameaggregator.vendor.esoterica.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.esoterica.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.esoterica.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.esoterica.response.ErrorResponse;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class EsotericaExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.AUTHENTICATION_FAILED);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.AUTHENTICATION_FAILED);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.INSUFFICIENT_FUNDS);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.AUTHENTICATION_FAILED);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.BET_NOT_ALLOWED);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCodes.DUPLICATE_REQUEST);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        ex.setShowFieldErrors(false);
        return getErrorResponse(ResponseCodes.INVALID_PARAMETER);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return getErrorResponse(ResponseCodes.BET_NOT_ALLOWED);
    }

    public VendorErrorResponse getErrorResponse(ResponseCodes responseCodes) {
        return new VendorErrorResponse(new ErrorResponse(responseCodes));
    }
}
