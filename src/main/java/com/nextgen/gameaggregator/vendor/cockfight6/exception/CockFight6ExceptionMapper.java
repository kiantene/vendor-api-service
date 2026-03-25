package com.nextgen.gameaggregator.vendor.cockfight6.exception;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cockfight6.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.cockfight6.response.FailResponse;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class CockFight6ExceptionMapper implements VendorExceptionMapper {
    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCode.SESSION_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCode.SESSION_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCode.INSUFFICIENT_BALANCE);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.INVALID_PLAYER);
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
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_REQUEST);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCode.BET_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_REFUND);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCode.BET_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCode.INVALID_REQUEST);
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCode.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(InvalidPlayerException.class) || ex.isCausedBy(PlayerNotFoundException.class)
                || ex.isCausedBy(EntityNotFoundException.class)) {
            return getErrorResponse(ResponseCode.INVALID_PLAYER);
        }
        return getErrorResponse(ResponseCode.INTERNAL_ERROR);

    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    public VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        return new VendorErrorResponse(FailResponse.builder()
                .code(responseCode.code)
                .message(responseCode.message)
                .build());
    }
}
