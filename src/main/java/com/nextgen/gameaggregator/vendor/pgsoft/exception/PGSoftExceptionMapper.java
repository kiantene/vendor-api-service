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

import java.math.BigDecimal;
import java.time.Instant;

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

    // applicable for promo payout (return success)
    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        ResponseVo<CashTransferInOutVo> responseVo = new ResponseVo<>();
        CashTransferInOutVo cashTransferInOutVo = new CashTransferInOutVo();
        cashTransferInOutVo.setUpdatedTime(Instant.now().toEpochMilli());
        cashTransferInOutVo.setBalanceAmount(BigDecimal.ZERO);
//        cashTransferInOutVo.setCurrencyCode("");
        responseVo.setData(cashTransferInOutVo);

        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }

    @Override
    public VendorErrorResponse onDuplicateBet(DuplicateBetException ex) {
        ResponseVo<CashTransferInOutVo> responseVo = new ResponseVo<>();
        CashTransferInOutVo cashTransferInOutVo = new CashTransferInOutVo();
        cashTransferInOutVo.setUpdatedTime(Instant.now().toEpochMilli());
        cashTransferInOutVo.setBalanceAmount(BigDecimal.ZERO);
//        cashTransferInOutVo.setCurrencyCode("");
        responseVo.setData(cashTransferInOutVo);

        return new VendorErrorResponse(HttpStatus.OK, responseVo);
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
        responseVo.setError(responseCodes);
        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }
}
