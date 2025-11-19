package com.nextgen.gameaggregator.vendor.mg.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.mg.api.betresult.UpdateBalanceVo;
import com.nextgen.gameaggregator.vendor.mg.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MGExceptionMapper implements VendorExceptionMapper {

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return createErrorResponse(HttpStatus.PAYMENT_REQUIRED);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        // For duplicate requests, return 200 OK (idempotent behavior)
        return createErrorResponse(HttpStatus.OK);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public String getVendorClassName() {
        return Vendors.MG.getClassName();
    }

    private VendorErrorResponse createErrorResponse(HttpStatus httpStatus) {
        return new VendorErrorResponse(httpStatus, new UpdateBalanceVo());
    }
}
