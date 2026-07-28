package com.nextgen.gameaggregator.vendor.koolbet.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.koolbet.response.CommonResponse;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME + "Rollback")
public class KoolbetRollbackExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME + "Rollback";
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCode.INVALID_PARAMETER);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCode.INVALID_PARAMETER);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCode.INSUFFICIENT_BALANCE);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.OTHER_ERROR);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCode.OTHER_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        CommonResponse response = new CommonResponse();
        response.setBalance(ex.getTransaction().getBalance());
        response.setUsername(ex.getTransaction().getUsername());
        response.setCurrency(ex.getTransaction().getCurrency());
        response.setErrorCode(ResponseCode.BET_ALREADY_ACCEPTED.code);
        return new VendorErrorResponse(ResponseCode.BET_ALREADY_ACCEPTED.httpStatus, response);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCode.INVALID_PARAMETER);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.isBetAlreadySettled()||ex.isRoundAlreadyEnded()) {
            return getErrorResponse(ResponseCode.ALREADY_ACCEPTED_AND_CANNOT_BE_CANCELED);
        }else if(ex.isBetNotFound()){
            return getErrorResponse(ResponseCode.ROUND_NOT_FOUND);
        }
        return getErrorResponse(ResponseCode.SESSION_BET_ALREADY_CANCELED);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCode.OTHER_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        Throwable rootCause = getRootCause(ex);

        if (ex.isCausedBy(InvalidOperatorResponseException.class)) {
            InvalidOperatorResponseException ioex = (InvalidOperatorResponseException) rootCause;

            Integer operatorStatus = ioex.getOperatorStatus();

            if (operatorStatus != null) {
                if (operatorStatus.equals(ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                    return getErrorResponse(ResponseCode.INSUFFICIENT_BALANCE);
                }
                return getErrorResponse(ResponseCode.OTHER_ERROR);
            }
        }
        return getErrorResponse(ResponseCode.OTHER_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        CommonResponse response = new CommonResponse();
        response.setErrorCode(responseCode.code);
        return new VendorErrorResponse(responseCode.httpStatus, response);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return getRootCause(cause);
        }
        return throwable;
    }
}
