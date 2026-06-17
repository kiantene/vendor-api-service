package com.nextgen.gameaggregator.vendor.topbet.exception;

import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.enums.GameRoundState;
import com.nextgen.gameaggregator.vendor.topbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.topbet.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.topbet.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.nextgen.gameaggregator.vendor.topbet.service.VendorUtil.formatBalance;

@Slf4j
@Component(EndPoints.CLASS_NAME)
public class TopbetExceptionMapper implements VendorExceptionMapper {

    private final WalletBalanceService walletBalanceService;

    public TopbetExceptionMapper(WalletBalanceService walletBalanceService) {
        this.walletBalanceService = walletBalanceService;
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        return getErrorResponse(ResponseCode.USER_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        ErrorResponse responseVo = ErrorResponse.builder()
                .code(ResponseCode.INSUFFICIENT_BALANCE.code)
                .message(ResponseCode.INSUFFICIENT_BALANCE.message)
                .balance(formatBalance(getPlayerBalanceDataByUserId(ex.getContext().getVendorPlayerUsername())))
                .build();
        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        if (ex.getTransaction().getState().equals(GameRoundState.REFUNDED)) {
            return getErrorResponse(ResponseCode.ORDER_ALREADY_ROLLED_BACK);
        }
        ErrorResponse responseVo = ErrorResponse.builder()
                .code(ResponseCode.DUPLICATE_REQUEST.code)
                .message(ResponseCode.DUPLICATE_REQUEST.message)
                .merchantTransId(ex.getTransaction().getTransactionId())
                .balance(ex.getTransaction().getBalance())
                .build();
        return new VendorErrorResponse(HttpStatus.OK, responseVo);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.isBetNotFound()) {
            ErrorResponse responseVo = ErrorResponse.builder()
                    .code(ResponseCode.SUCCESS.code)
                    .message(ResponseCode.SUCCESS.message)
                    .merchantTransId("")
                    .build();
            return new VendorErrorResponse(HttpStatus.OK, responseVo);
        }
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        return getErrorResponse(ResponseCode.SYSTEM_ERROR);
    }

    private VendorErrorResponse getErrorResponse(ResponseCode responseCode) {
        return new VendorErrorResponse(responseCode.httpStatus, new ErrorResponse(responseCode));
    }

    private BigDecimal getPlayerBalanceDataByUserId(String userId) {
        BalanceContext balanceContext = null;

        try {
            if (userId != null) {
                balanceContext = BalanceContext.builder()
                        .vendorPlayerUsername(userId)
                        .build();
            }
        }
        catch (Exception e) {
            log.error(ResponseCode.USER_NOT_FOUND.getMessage());
        }

        return walletBalanceService.process(balanceContext).getBalance();
    }
}