package com.nextgen.gameaggregator.vendor.groove.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.groove.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.groove.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.groove.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class GrooveExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCode.NOT_LOGGED_ON);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCode.NOT_LOGGED_ON);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCode.OUT_OF_MONEY);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.ACCOUNT_BLOCKED);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCode.ROUND_CLOSED_OR_DUPLICATE_TXN);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setCode(ResponseCode.DUPLICATE_SUCCESS.code);
        response.setStatus(ResponseCode.DUPLICATE_SUCCESS.message);
        return new VendorErrorResponse(HttpStatus.OK, response);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        if (ex.isRoundAlreadyEnded()) {
            return getErrorResponse(ResponseCode.ROUND_CLOSED_OR_DUPLICATE_TXN);
        }
        return getErrorResponse(ResponseCode.OPERATION_NOT_ALLOWED);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        ex.setShowFieldErrors(false);
        return getErrorResponse(ResponseCode.PARAMETER_REQUIRED);
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCode.TECHNICAL_ERROR);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.isBetNotFound()) {
            return getErrorResponse(ResponseCode.WAGER_NOT_FOUND);
        }
        if(ex.isBetAlreadySettled())
        {
            return getErrorResponse(ResponseCode.ROUND_CLOSED_OR_DUPLICATE_TXN);
        }
        return getErrorResponse(ResponseCode.OPERATION_NOT_ALLOWED);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCode.WAGER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(BetNotFoundException.class)) {
            return getErrorResponse(ResponseCode.ROUND_CLOSED_OR_DUPLICATE_TXN);
        }

        return getErrorResponse(ResponseCode.TECHNICAL_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        ErrorResponse response = new ErrorResponse();
        response.setError(responseCode);
        return new VendorErrorResponse(responseCode.httpStatus, response);
    }
}