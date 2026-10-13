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
        // Used by Step 5 cutover (JdbPreflightService) for both AuthenticationException
        // and InvalidPlayerException paths — both legacy services mapped these to PLAYER_NOT_FOUND.
        return getErrorResponse(ResponseCode.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        throw new UnsupportedOperationException("Unsupported exception: " + ex.getClass().getName());
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCode.INSUFFICIENT_BALANCE);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.PLAYER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCode.FAILED);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCode.DUPLICATE_TRANSACTION);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        // Defensive: not expected to fire when the wrapper's returnSuccessOnDuplicate is true
        // (idempotent replays are handled upstream as success + prior balance).
        return getErrorResponse(ResponseCode.DUPLICATE_TRANSACTION);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCode.FAILED);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        // Action 11 naming a debit that is already closed — settled, or cancelled under another
        // transferId. Either way the cancel cannot be applied. A replay of the same transferId never
        // reaches here: it is answered 0000 from the recorded result.
        if (ex.isBetAlreadySettled() || ex.getCause() instanceof RoundAlreadyRefundedException) {
            return getErrorResponse(ResponseCode.CANNOT_CANCEL);
        }
        return getErrorResponse(ResponseCode.FAILED);
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
