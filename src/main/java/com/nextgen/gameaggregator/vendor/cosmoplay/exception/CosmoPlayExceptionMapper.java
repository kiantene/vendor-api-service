package com.nextgen.gameaggregator.vendor.cosmoplay.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.config.CosmoPlayVendorConfig;
import com.nextgen.gameaggregator.vendor.cosmoplay.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.cosmoplay.response.ResponseCode;
import org.springframework.stereotype.Component;

@Component(CosmoPlayVendorConfig.CLASS_NAME)
public class CosmoPlayExceptionMapper implements VendorExceptionMapper {
    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return ErrorResponse.of(
                ResponseCode.rollback(ex),
                ex.getMessage()
        );
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return ErrorResponse.of(ResponseCode.DEADLINE_EXCEEDED, ex.getMessage());
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return ErrorResponse.of(ResponseCode.UNAVAILABLE, ex.getMessage());
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        Throwable cause = ex.getRootCause();

        return ErrorResponse.of(
                ResponseCode.BALANCE_INSUFFICIENT,
                ex.getMessage() + ". Caused by: " + cause.getMessage() + "."
        );
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return ErrorResponse.of(ResponseCode.PERMISSION_DENIED, ex.getMessage());
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return ErrorResponse.of(
                ResponseCode.betNotAllowed(ex),
                ex.getMessage()
        );
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return ErrorResponse.of(ResponseCode.ALREADY_EXISTS, ex.getMessage());
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return ErrorResponse.of(
                ResponseCode.betResultRejected(ex),
                ex.getMessage()
        );
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return ErrorResponse.of(ResponseCode.ABORTED, ex.getMessage());
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return ErrorResponse.of(ResponseCode.INTERNAL, ex.getMessage());
    }

    @Override
    public String getVendorClassName() {
        return CosmoPlayVendorConfig.CLASS_NAME;
    }
}
