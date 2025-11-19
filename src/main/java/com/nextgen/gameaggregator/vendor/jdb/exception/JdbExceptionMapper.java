package com.nextgen.gameaggregator.vendor.jdb.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.Vendors;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.springframework.stereotype.Component;

@Component
public class JdbExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return Vendors.JDB.getClassName();
    }
    // applicable for promo payout
    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCode.FAILED);
    }

    // applicable for promo payout
    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCode.FAILED);
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
        return getErrorResponse(ResponseCode.DUPLICATE_TRANSACTION);
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
        return getErrorResponse(ResponseCode.INVALID_REQUEST_PARAMETER);
    }


    private VendorErrorResponse getErrorResponse(String responseCode) {
        CommonVo response = new CommonVo();
        response.setErrorResponseCode(responseCode);
        return new VendorErrorResponse(response);
    }
}
