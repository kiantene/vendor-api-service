package com.nextgen.gameaggregator.vendor.pragmaticplayv2.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.freeround.FreeRoundPayoutResponse;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplayv2.response.ErrorResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component(Endpoints.CLASS_NAME)
public class PragmaticPlayExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME;
    }

    private VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        return new VendorErrorResponse(new ErrorResponse(responseCode));
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        FreeRoundPayoutResponse response = FreeRoundPayoutResponse.builder()
                .transactionId(ex.getTransactionId())
                .currency(ex.getCurrency())
                .cash(ex.getBalance())
                .bonus(BigDecimal.ZERO)
                .build();

        return new VendorErrorResponse(response);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCode.INVALID_REQUEST);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
    }
}
