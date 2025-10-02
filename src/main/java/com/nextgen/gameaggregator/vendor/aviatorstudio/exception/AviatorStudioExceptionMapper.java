package com.nextgen.gameaggregator.vendor.aviatorstudio.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.aviatorstudio.response.ErrorResponse;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class AviatorStudioExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return ErrorResponse.of(ResponseCodes.AUTH_ERROR);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return ErrorResponse.of(ResponseCodes.AUTH_ERROR);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return ErrorResponse.of(ResponseCodes.INSUFFICIENT_FUNDS);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return ErrorResponse.of(ResponseCodes.AUTH_ERROR);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return ErrorResponse.of(ResponseCodes.AUTH_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return ErrorResponse.of(ResponseCodes.SERVER_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return ErrorResponse.of(ResponseCodes.SERVER_ERROR);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return ErrorResponse.of(ResponseCodes.SERVER_ERROR);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return ErrorResponse.of(ResponseCodes.SERVER_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return ErrorResponse.of(ResponseCodes.SERVER_ERROR);
    }
}
