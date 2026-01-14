package com.nextgen.gameaggregator.vendor.superbullgaming.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.superbullgaming.config.SuperBullGamingConfig;
import com.nextgen.gameaggregator.vendor.superbullgaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.superbullgaming.vo.CommonVo;
import org.springframework.stereotype.Component;

@Component
public class SBGExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return SuperBullGamingConfig.CLASS_NAME;
    }
    // applicable for promo payout
    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCode.OPERATION_FAILED);
    }

    // applicable for promo payout
    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCode.OPERATION_FAILED);
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
        return getErrorResponse(ResponseCode.OPERATION_FAILED);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCode.INVALID_REQUEST);
    }


    private VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        CommonVo response = new CommonVo();
        response.setResponseCode(responseCode);
        return new VendorErrorResponse(response);
    }
}
