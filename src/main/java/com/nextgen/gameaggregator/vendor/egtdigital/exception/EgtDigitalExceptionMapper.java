package com.nextgen.gameaggregator.vendor.egtdigital.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.egtdigital.api.result.BetResultResponse;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.egtdigital.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component(EndPoints.CLASS_NAME)
public class EgtDigitalExceptionMapper implements VendorExceptionMapper {
    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_INVALID_TOKEN));
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_UNKNOWN));
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_NOT_ENOUGH_MONEY));
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_INVALID_PLAYER_ID));
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_UNKNOWN));
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        BetResultResponse betResultResponse= BetResultResponse.builder()
                .balance(Amount.vendor(ex.getTransaction().getBalance()))
                .bonusAmount(Amount.vendor(ex.getTransaction().getJackpotAmount()))
                .realAmount(Amount.vendor(ex.getTransaction().getWinAmount()))
                .casinoTransferId(ex.getTransaction().getVendorBetId())
                .statusCode(ResponseCodes.OK.getCode())
                .build();

        return new VendorErrorResponse(HttpStatus.OK, betResultResponse);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        if (ex.isCausedBy(RoundAlreadyEndedException.class)) {
            return new VendorErrorResponse(HttpStatus.OK, BetResultResponse.builder()
                    .statusCode(ResponseCodes.OK.getCode()).build());
        }
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_UNKNOWN));
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_UNKNOWN));
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return new VendorErrorResponse(HttpStatus.OK, errorResponse(ResponseCodes.ERR_UNKNOWN));
    }

    public ErrorResponse errorResponse(ResponseCodes responseCodes) {
        return new ErrorResponse(responseCodes);
    }
}