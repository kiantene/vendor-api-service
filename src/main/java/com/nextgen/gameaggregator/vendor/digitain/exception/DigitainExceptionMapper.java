package com.nextgen.gameaggregator.vendor.digitain.exception;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.vendor.digitain.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.digitain.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.digitain.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component(EndPoints.CLASS_NAME)
public class DigitainExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.SESSION_NOT_FOUND_OR_EXPIRED);
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.GAME_NOT_FOUND);
        errorResponse.setTxid(ex.getTransactionId());
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.LOW_BALANCE);
        errorResponse.setTxid(ex.getTransactionId());
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.WRONG_PLAYER_ID);
        errorResponse.setTxid(ex.getTransactionId());
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        if(ex.isCausedBy(DisabledGameException.class)){
            ErrorResponse errorResponse = new ErrorResponse(ResponseCode.SESSION_NOT_FOUND_OR_EXPIRED);
            errorResponse.setTxid(ex.getTransactionId());
            return new VendorErrorResponse(HttpStatus.OK, errorResponse);
        }
        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.GENERAL_ERROR);
        errorResponse.setTxid(ex.getTransactionId());
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        BigDecimal balance = (ex.getTransaction() != null && ex.getTransaction().getBalance() != null)
                ? ex.getTransaction().getBalance().setScale(4, RoundingMode.DOWN)
                : BigDecimal.ZERO;

        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.TRANSACTION_ALREADY_EXISTS);
        errorResponse.setTxid(ex.getTransaction().getTransactionId());
        errorResponse.setBln(balance);
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if(ex.isBetAlreadySettled()){
            ErrorResponse errorResponse = new ErrorResponse(ResponseCode.GENERAL_ERROR);
            errorResponse.setTxid(ex.getTransactionId());
            return new VendorErrorResponse(HttpStatus.OK, errorResponse);
        }
        ErrorResponse errorResponse = new ErrorResponse(ResponseCode.TRANSACTION_NOT_FOUND);
        errorResponse.setTxid(ex.getTransactionId());
        return new VendorErrorResponse(HttpStatus.OK, errorResponse);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {

        if (ex.isRoundNotFound() || ex.isBetNotFound() ) {
            ErrorResponse errorResponse = new ErrorResponse(ResponseCode.TRANSACTION_NOT_FOUND);
            errorResponse.setTxid(ex.getTransactionId());
            return new VendorErrorResponse(HttpStatus.OK, errorResponse);
        }
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCode.GENERAL_ERROR));
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCode.GENERAL_ERROR));
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(EntityNotFoundException.class))
        {
            ErrorResponse errorResponse = new ErrorResponse(ResponseCode.WRONG_PLAYER_ID);
            return new VendorErrorResponse(HttpStatus.OK, errorResponse);
        }
        else if (ex.isCausedBy(GameNotSupportedException.class) || ex.isCausedBy(InternalConfigurationException.class))
        {
            ErrorResponse errorResponse = new ErrorResponse(ResponseCode.GAME_NOT_FOUND);
            return new VendorErrorResponse(HttpStatus.OK, errorResponse);
        }
        return new VendorErrorResponse(HttpStatus.OK, new ErrorResponse(ResponseCode.GENERAL_ERROR));
    }

}