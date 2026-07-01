package com.nextgen.gameaggregator.vendor.mtlive.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.vendor.mtlive.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.mtlive.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.mtlive.response.ErrorResponse;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class MtliveExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCode.ORDER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCode.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCode.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCode.INSUFFICIENT_BALANCE);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.EXECUTION_FAILED);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCode.EXECUTION_FAILED);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_ORDER);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if(ex.isBetNotFound()){
            return getErrorResponse(ResponseCode.ORDER_NOT_FOUND);
        }
        return getErrorResponse(ResponseCode.ORDER_CANCELED);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        if(ex.isBetNotFound()||ex.isRoundNotFound()){
            return getErrorResponse(ResponseCode.ORDER_NOT_FOUND);
        } else if (ex.isCausedBy(InvalidPlayerException.class)) {
            return getErrorResponse(ResponseCode.PLAYER_NOT_FOUND);
        } else if (ex.isCausedBy(RoundAlreadyRefundedException.class)){
            return getErrorResponse(ResponseCode.ORDER_CANCELED);
        }
        return getErrorResponse(ResponseCode.ORDER_SETTLED);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        ex.setShowFieldErrors(false);
        return getErrorResponse(ResponseCode.INVALID_PARAMETER);
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCode.EXECUTION_FAILED);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if(ex.isCausedBy(BetResultNotFoundException.class)||ex.isCausedBy(RoundNotFoundException.class)){
            return getErrorResponse(ResponseCode.ORDER_NOT_FOUND);
        } else if (ex.isCausedBy(InvalidPlayerException.class)){
            return getErrorResponse(ResponseCode.PLAYER_NOT_FOUND);
        }
        return getErrorResponse(ResponseCode.EXECUTION_FAILED);
    }

    private VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        ErrorResponse errorResponse = new ErrorResponse(responseCode);
        return new VendorErrorResponse(responseCode.httpStatus, errorResponse);
    }
}