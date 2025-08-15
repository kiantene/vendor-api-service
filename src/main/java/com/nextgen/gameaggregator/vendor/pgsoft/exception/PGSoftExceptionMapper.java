package com.nextgen.gameaggregator.vendor.pgsoft.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorExceptionMapper;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import org.springframework.stereotype.Component;

@Component
public class PGSoftExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return null;
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return null;
    }
}
