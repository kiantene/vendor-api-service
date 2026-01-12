package com.nextgen.gameaggregator.vendor.spribe.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.spribe.config.SpribeConfig;
import com.nextgen.gameaggregator.vendor.spribe.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.spribe.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component(SpribeConfig.CLASS_NAME)
public class SpribeExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return SpribeConfig.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ErrorCodes.EXPIRED_TOKEN);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ErrorCodes.INVALID_TOKEN);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ErrorCodes.INSUFFICIENT_FUND);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ErrorCodes.INTERNAL_ERROR_NO_RETRY);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ErrorCodes.INTERNAL_ERROR_NO_RETRY);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ErrorCodes.DUPLICATE_TRANSACTION);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ErrorCodes.INTERNAL_ERROR_NO_RETRY);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return getErrorResponse(ErrorCodes.SUCCESS);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ErrorCodes.INTERNAL_ERROR_NO_RETRY);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ErrorCodes.INTERNAL_ERROR_NO_RETRY);
    }

    private VendorErrorResponse getErrorResponse(ErrorCodes errorCode) {
        ErrorResponse errorResponse = ErrorResponse.of(errorCode);

        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }
}
