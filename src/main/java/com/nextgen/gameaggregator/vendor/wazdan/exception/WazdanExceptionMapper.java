package com.nextgen.gameaggregator.vendor.wazdan.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.wazdan.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.wazdan.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.wazdan.response.ErrorResponse;
import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.concurrent.TimeoutException;

@Component(EndPoints.CLASS_NAME)
public class WazdanExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCode.SESSION_EXPIRED);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCode.SESSION_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCode.INSUFFICIENT_FUNDS);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.USER_BLOCKED);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {

        if(ex.getTransaction() == null){
            return getErrorResponse(ResponseCode.SYSTEM_ERROR);
        }

        ErrorResponse responseVo = ErrorResponse.builder()
                .status(ResponseCode.SUCCESS.code)
                .funds(ErrorResponse.Funds.builder()
                        .balance(ex.getTransaction().getBalance().setScale(2, RoundingMode.DOWN))
                        .build())
                .errorMessage(ResponseCode.DUPLICATE_REQUEST.message)
                .build();
        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(ReadTimeoutException.class)||ex.isCausedBy(TimeoutException.class)) {
            ErrorResponse error = ErrorResponse.builder()
                    .errorMessage(ResponseCode.SYSTEM_ERROR.message)
                    .build();
            return new VendorErrorResponse(HttpStatus.OK, error);
        }
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        ErrorResponse error = ErrorResponse.builder()
                .status(responseCode.code)
                .errorMessage(responseCode.message)
                .build();
        return new VendorErrorResponse(responseCode.httpStatus, error);
    }
}