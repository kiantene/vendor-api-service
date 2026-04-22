package com.nextgen.gameaggregator.vendor.hp100.exception;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.vendor.hp100.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.hp100.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.hp100.response.FailResponse;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Component;

@Component(Endpoints.CLASS_NAME)
public class Hp100ExceptionMapper implements VendorExceptionMapper {
    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCode.AUTHENTICATION_FAILED);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_REQUEST);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCode.AUTHENTICATION_FAILED);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCode.INSUFFICIENT_BALANCE);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.AUTHENTICATION_FAILED);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCode.BET_REJECTED);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_REQUEST);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        if (ex.isRoundAlreadyRefunded()) {
            return getErrorResponse(ResponseCode.DUPLICATE_REFUND);
        }
        if (ex.isBetNotFound() || ex.isRoundNotFound()) {
            return getErrorResponse(ResponseCode.BET_NOT_FOUND);
        }
        return getErrorResponse(ResponseCode.DUPLICATE_SETTLE);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        ex.setShowFieldErrors(false);
        return getErrorResponse(ResponseCode.INVALID_REQUEST);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.isBetAlreadySettled()) {
            return getErrorResponse(ResponseCode.DUPLICATE_SETTLE);
        }
        return getErrorResponse(ResponseCode.BET_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCode.BET_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(InvalidPlayerException.class) || ex.isCausedBy(EntityNotFoundException.class)) {
            return getErrorResponse(ResponseCode.AUTHENTICATION_FAILED);
        }
        if (ex.isCausedBy(InvalidDataAccessApiUsageException.class)) {
            return getErrorResponse(ResponseCode.AUTHENTICATION_FAILED);
        }
        return getErrorResponse(ResponseCode.INTERNAL_ERROR);

    }

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME;

    }

    public VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        FailResponse response = new FailResponse(responseCode);
        return new VendorErrorResponse(
                responseCode.httpStatus,
                response
        );

    }

}
