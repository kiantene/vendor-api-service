package com.nextgen.gameaggregator.vendor.pgsoft.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutVo;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PGSoftExceptionMapper implements VendorExceptionMapper {

    @Override
    public String getVendorClassName() {
        return Endpoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCodes.INVALID_PLAYER_SESSION_1300);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.INVALID_PLAYER_SESSION_1300);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.NOT_ENOUGH_CASH_BALANCE_TO_BET);
    }

    // applicable for promo payout
    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCodes.OPERATION_FAILED);
    }

    // applicable for promo payout (return success)
    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.INVALID_PLAYER_SESSION_1300);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.BET_FAILED_3073);
    }

    /**
     * Promo payout answers its own duplicates, in PGSoftPromoPayoutService, because a duplicate response
     * has to carry real_transfer_amount and updated_time from the request — neither of which reaches a
     * mapper, which is handed only the exception.
     *
     * <p>So this is the fallback for a duplicate arriving by some other route. It cannot build the
     * response PG SOFT validates, and a success body missing those fields is what broke free-round
     * resumption in the first place, so ask for the request again rather than answer it incompletely.
     */
    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS);
    }

    /**
     * Not reachable today: DuplicateBetException comes only from WalletExceptionTranslator, and neither
     * PGSoft branch goes through it — the bet path handles its own exceptions inline and promo payout
     * does not use the wallet wrappers. Kept safe rather than left returning a zero balance as success.
     */
    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        return getErrorResponse(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.OPERATION_FAILED);
    }

    // applicable for promo payout
    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCodes.INVALID_REQUEST);
    }

    // applicable for promo payout
    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCodes.OPERATION_FAILED);
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes responseCodes) {
        ResponseVo<CashTransferInOutVo> responseVo = new ResponseVo<>();
        responseVo.setData(null);
        responseVo.setError(responseCodes);
        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }
}
