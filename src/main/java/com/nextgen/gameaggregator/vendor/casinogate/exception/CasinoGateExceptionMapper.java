package com.nextgen.gameaggregator.vendor.casinogate.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.BetResultRejectedException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.GameTerminatedException;
import com.nextgen.gameaggregator.core.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.core.exception.PlayerDisabledException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.casinogate.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.casinogate.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.casinogate.response.ErrorResponse;
import org.springframework.stereotype.Component;

@Component(Endpoints.CLASS_NAME)
public class CasinoGateExceptionMapper implements VendorExceptionMapper {

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return error(ResponseCodes.SESSION_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return error(ResponseCodes.SESSION_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return error(ResponseCodes.INSUFFICIENT_FUNDS);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return error(ResponseCodes.SESSION_VALIDATION_FAILED);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        if (ex.isMultipleBetNotAllowed()) {
            return error(ResponseCodes.TRANSACTION_ALREADY_PROCESSED);
        }

        if (ex.isRoundAlreadyEnded()) {
            return error(ResponseCodes.GAME_ROUND_ALREADY_CLOSED);
        }

        return error(ResponseCodes.ROUND_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        if (ex.getTransaction() != null && ex.getTransaction().isRollback()) {
            return error(ResponseCodes.TRANSACTION_ALREADY_REFUNDED);
        }

        return error(ResponseCodes.TRANSACTION_ALREADY_PROCESSED);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return error(ResponseCodes.BET_TRANSACTION_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.isBetNotFound()) {
            return error(ResponseCodes.BET_TRANSACTION_NOT_FOUND);
        }

        return error(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        if (ex.isRoundAlreadyEnded()) {
            return error(ResponseCodes.GAME_ROUND_ALREADY_CLOSED);
        }

        if (ex.isRoundAlreadyRefunded()) {
            return error(ResponseCodes.TRANSACTION_ALREADY_REFUNDED);
        }

        return error(ResponseCodes.ROUND_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        ex.setShowFieldErrors(false);
        return error(ResponseCodes.INVALID_REQUEST);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return error(ResponseCodes.INTERNAL_ERROR);
    }

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME;
    }

    private VendorErrorResponse error(ResponseCodes responseCode) {
        ErrorResponse response = ErrorResponse.builder()
                .code(responseCode.getCode())
                .description(responseCode.getDescription())
                .build();

        return new VendorErrorResponse(responseCode.getHttpStatus(), response);
    }
}
