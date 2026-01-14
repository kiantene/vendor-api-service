package com.nextgen.gameaggregator.vendor.lucky365.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.lucky365.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.lucky365.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.lucky365.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.lucky365.util.Lucky365Exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;

@Component(EndPoints.CLASS_NAME)
public class Lucky365ExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.INSUFFICIENT_BALANCE);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCodes.BET_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCodes.DUPLICATE_REQUEST);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCodes.INVALID_PARAMETER);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if(ex.isCausedBy(ConstraintViolationException.class))
        {
            return getErrorResponse(ResponseCodes.INVALID_PARAMETER);
        }
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes responseCodes) {

        if (Lucky365Exception.isListResponse()) {

            return new VendorErrorResponse(
                    responseCodes.getHttpStatus(),
                    List.of(ErrorResponse.of(responseCodes))
            );
        }
        return new VendorErrorResponse(
                responseCodes.getHttpStatus(),
                ErrorResponse.of(responseCodes)

        );
    }
}

