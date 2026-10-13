package com.nextgen.gameaggregator.vendor.megafair.exception;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.exception.mapper.VendorExceptionMapper;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.vendor.megafair.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.megafair.constant.Formats;
import com.nextgen.gameaggregator.vendor.megafair.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.megafair.constant.Results;
import com.nextgen.gameaggregator.vendor.megafair.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component(EndPoints.CLASS_NAME)
public class MegafairExceptionMapper implements VendorExceptionMapper {

    private final VendorUtil vendorUtil;
    private static final DateTimeFormatter VENDOR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(Formats.DATE_TIME_FORMAT).withLocale(Locale.ENGLISH);

    public MegafairExceptionMapper(VendorUtil vendorUtil) {
        this.vendorUtil = vendorUtil;
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse onGameSessionExpired(GameSessionExpiredException ex) {
        if (ex.isCausedBy(PlayerNotFoundException.class)) {
            return getErrorResponse(ResponseCodes.UNKNOWN_USER_ID);
        }
        return getErrorResponse(ResponseCodes.SESSION_NOT_FOUND);
    }

    @Override
    public VendorErrorResponse onGameTerminated(GameTerminatedException ex) {
        return getErrorResponse(ResponseCodes.INVALID_TOKEN);
    }

    @Override
    public VendorErrorResponse onInsufficientBalance(InsufficientBalanceException ex) {
        return getErrorResponse(ResponseCodes.INSUFFICIENT_FUNDS);
    }

    @Override
    public VendorErrorResponse onPlayerDisabled(PlayerDisabledException ex) {
        return getErrorResponse(ResponseCodes.ACCESS_DENIED);
    }

    @Override
    public VendorErrorResponse onBetNotAllowed(BetNotAllowedException ex) {
        return getErrorResponse(ResponseCodes.OPERATION_FAILED);
    }

    @Override
    public VendorErrorResponse onDuplicateRequest(DuplicateRequestException ex) {
        var tx = ex.getTransaction();

        String extSystemTransactionId = (tx != null) ? tx.getVendorBetId() : null;
        BigDecimal balance = null;

        if (tx != null && tx.getUsername() != null) {
            balance = vendorUtil.getPlayerBalanceDataByUserId(tx.getUsername());
        } else {
            log.warn("Duplicate request handled without full transaction/balance context in exception", ex);
        }

        // Fall back to exception's stored major unit balance if wallet lookup returned null or wasn't executable
        if (balance == null) {
            balance = ex.getBalance();
        }

        ErrorResponse duplicateResponse = ErrorResponse.builder()
                .time(VENDOR_DATE_FORMATTER.format(ZonedDateTime.now()))
                .response(
                        ErrorResponse.ResponseData.builder()
                                .result(Results.SUCCESS)
                                .extSystemTransactionId(extSystemTransactionId)
                                .balance(balance)
                                .build()
                )
                .build();

        return new VendorErrorResponse(ResponseCodes.SUCCESS.getHttpStatus(), duplicateResponse);
    }

    @Override
    public VendorErrorResponse onRollbackNotAllowed(RollbackNotAllowedException ex) {
        if (ex.isCausedBy(BetNotFoundException.class)) {
            return getErrorResponse(ResponseCodes.UNKNOWN_TRANSACTION_ID);
        }
        return getErrorResponse(ResponseCodes.OPERATION_FAILED);
    }

    @Override
    public VendorErrorResponse onBetResultRejected(BetResultRejectedException ex) {
        return getErrorResponse(ResponseCodes.OPERATION_FAILED);
    }

    @Override
    public VendorErrorResponse onInvalidRequestError(InvalidRequestException ex) {
        ex.setShowFieldErrors(false);
        Map<String, String> fieldErrors = ex.getFieldErrors();

        if (fieldErrors != null) {
            // Check for space in gameSessionId -> Return 612
            if (fieldErrors.containsKey("gameSessionIdValid")) {
                return getErrorResponse(ResponseCodes.SESSION_NOT_FOUND);
            }

            // Both bet AND win were provided -> Return 609 (REQUEST_SYNTAX_MISMATCH)
            if (fieldErrors.containsKey("notBothBetAndWin")) {
                return getErrorResponse(ResponseCodes.REQUEST_SYNTAX_MISMATCH);
            }
        }

        if ("Invalid VendorPlayerUsername".equals(ex.getMessage())) {
            return getErrorResponse(ResponseCodes.USER_NOT_FOUND);
        }

        // Default fallback for other invalid parameters -> Return 610
        return getErrorResponse(ResponseCodes.INVALID_PARAMETERS);
    }

    @Override
    public VendorErrorResponse onInternalConfigurationError(InternalConfigurationException ex) {
        if (ex.getCause() instanceof EntityNotFoundException) {
            return getErrorResponse(ResponseCodes.USER_NOT_FOUND);
        }
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR_ES);
    }

    @Override
    public VendorErrorResponse onBetNotFound(BetNotFoundException ex) {
        return getErrorResponse(ResponseCodes.UNKNOWN_TRANSACTION_ID);
    }

    @Override
    public VendorErrorResponse onInternalError(InternalServerException ex) {
        if (ex.isCausedBy(InvalidPlayerException.class)) {
            return getErrorResponse(ResponseCodes.USER_NOT_FOUND);
        }
        return getErrorResponse(ResponseCodes.INTERNAL_ERROR_ES);
    }

    private VendorErrorResponse getErrorResponse(ResponseCodes response) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .time(VENDOR_DATE_FORMATTER.format(ZonedDateTime.now()))
                .response(
                        ErrorResponse.ResponseData.builder()
                                .result(Results.ERROR)
                                .code(String.valueOf(response.getCode()))
                                .build()
                )
                .build();
        return new VendorErrorResponse(response.getHttpStatus(), errorResponse);
    }
}
