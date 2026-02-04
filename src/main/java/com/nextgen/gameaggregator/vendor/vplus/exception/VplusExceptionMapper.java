package com.nextgen.gameaggregator.vendor.vplus.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.vendor.vplus.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.vplus.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.vplus.response.ErrorResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component(EndPoints.CLASS_NAME)
public class VplusExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.GAME_NOT_FOUND_OR_DISABLED);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.GAME_NOT_FOUND_OR_DISABLED);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.BETTING_LIMIT_REACHED);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_FROZEN);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.BETTING_NOT_ALLOWED);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        BigDecimal balance = ex.getTransaction().getBalance();
        ResponseCodes responseCode = ResponseCodes.DUPLICATE_REQUEST;

        ErrorResponse responseVo = new ErrorResponse();
        responseVo.setCode(responseCode.getCode());
        responseVo.setMessage(responseCode.getMessage());
        responseVo.setBalance(String.valueOf(balance));

        return new VendorErrorResponse(responseCode.getHttpStatus(), responseVo);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.isBetNotFound()) {
            return getErrorResponse(ResponseCodes.OTHER_ERROR);
        }

        return getErrorResponse(ResponseCodes.BET_CONFIRMED_NOT_CANCELABLE);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.OTHER_ERROR);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCodes.INVALID_REQUEST_PARAMETERS);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(GameNotSupportedException.class)) {
            return getErrorResponse(ResponseCodes.GAME_NOT_FOUND_OR_DISABLED);
        }
        return getErrorResponse(ResponseCodes.OTHER_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes responseCode) {
        return new VendorErrorResponse(responseCode.getHttpStatus(), ErrorResponse.of(responseCode));
    }
}

