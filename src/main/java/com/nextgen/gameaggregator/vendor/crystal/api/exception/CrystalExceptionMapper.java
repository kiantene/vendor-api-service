package com.nextgen.gameaggregator.vendor.crystal.api.exception;


import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.crystal.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.crystal.vo.ErrorVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class CrystalExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.INVALID_SIGNATURE, HttpStatus.FORBIDDEN);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.INVALID_SIGNATURE, HttpStatus.FORBIDDEN);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.INSUFFICIENT_FUNDS, HttpStatus.BAD_REQUEST);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.INVALID_SIGNATURE, HttpStatus.FORBIDDEN);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND, HttpStatus.FORBIDDEN);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND, HttpStatus.FORBIDDEN);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCodes.INVALID_PARAMETERS, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes responseCode, HttpStatus httpStatus) {
        return new VendorErrorResponse(httpStatus, new ErrorVo(responseCode.code, responseCode.message));
    }
}