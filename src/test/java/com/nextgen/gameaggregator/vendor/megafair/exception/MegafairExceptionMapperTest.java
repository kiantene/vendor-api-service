package com.nextgen.gameaggregator.vendor.megafair.exception;

import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.exception.BetNotFoundException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.vendor.megafair.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.megafair.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MegafairExceptionMapperTest {

    @Mock
    private VendorUtil vendorUtil;

    @Mock
    private VendorRequestContext vendorRequestContext;

    private MegafairExceptionMapper exceptionMapper;

    @BeforeEach
    void setUp() {
        exceptionMapper = new MegafairExceptionMapper(vendorUtil);
    }

    @Test
    @DisplayName("onGameSessionExpired: Maps caused by PlayerNotFoundException to UNKNOWN_USER_ID (309)")
    void onGameSessionExpired_CausedByPlayerNotFound_ReturnsUnknownUserId() {
        PlayerNotFoundException cause = new PlayerNotFoundException("Player not found");
        GameSessionExpiredException ex = new GameSessionExpiredException(vendorRequestContext, "Session expired");
        ex.initCause(cause);

        VendorErrorResponse response = exceptionMapper.onGameSessionExpired(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.UNKNOWN_USER_ID.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.UNKNOWN_USER_ID.getCode()));
    }

    @Test
    @DisplayName("onGameSessionExpired: Maps default expired session to SESSION_NOT_FOUND (612)")
    void onGameSessionExpired_Default_ReturnsSessionNotFound() {
        GameSessionExpiredException ex = new GameSessionExpiredException(vendorRequestContext, "Session expired");

        VendorErrorResponse response = exceptionMapper.onGameSessionExpired(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.SESSION_NOT_FOUND.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.SESSION_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("onRollbackNotAllowed: Maps caused by BetNotFoundException to UNKNOWN_TRANSACTION_ID (302)")
    void onRollbackNotAllowed_CausedByBetNotFound_ReturnsUnknownTransactionId() {
        BetNotFoundException cause = new BetNotFoundException("Bet not found");
        RollbackNotAllowedException ex = new RollbackNotAllowedException(vendorRequestContext, cause);

        VendorErrorResponse response = exceptionMapper.onRollbackNotAllowed(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.UNKNOWN_TRANSACTION_ID.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.UNKNOWN_TRANSACTION_ID.getCode()));
    }

    @Test
    @DisplayName("onRollbackNotAllowed: Maps generic rollback error to OPERATION_FAILED (301)")
    void onRollbackNotAllowed_GenericCause_ReturnsOperationFailed() {
        RollbackNotAllowedException ex = new RollbackNotAllowedException("Rollback not allowed");

        VendorErrorResponse response = exceptionMapper.onRollbackNotAllowed(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.OPERATION_FAILED.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.OPERATION_FAILED.getCode()));
    }

    @Test
    @DisplayName("onInvalidRequestError: Maps gameSessionIdValid field error to SESSION_NOT_FOUND (612)")
    void onInvalidRequestError_GameSessionIdValid_ReturnsSessionNotFound() {
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("gameSessionIdValid", "must not contain spaces");
        InvalidRequestException ex = new InvalidRequestException("Invalid request", fieldErrors);

        VendorErrorResponse response = exceptionMapper.onInvalidRequestError(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.SESSION_NOT_FOUND.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.SESSION_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("onInvalidRequestError: Maps notBothBetAndWin field error to REQUEST_SYNTAX_MISMATCH (609)")
    void onInvalidRequestError_NotBothBetAndWin_ReturnsRequestSyntaxMismatch() {
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("notBothBetAndWin", "Request cannot contain both 'bet' and 'win'");
        InvalidRequestException ex = new InvalidRequestException("Invalid request", fieldErrors);

        VendorErrorResponse response = exceptionMapper.onInvalidRequestError(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.REQUEST_SYNTAX_MISMATCH.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.REQUEST_SYNTAX_MISMATCH.getCode()));
    }

    @Test
    @DisplayName("onInvalidRequestError: Maps betOrWinPresent field error to fallback INVALID_PARAMETERS (610)")
    void onInvalidRequestError_BetOrWinPresent_ReturnsInvalidParameters() {
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("betOrWinPresent", "Request must contain at least one of 'bet' or 'win'");
        InvalidRequestException ex = new InvalidRequestException("Invalid request", fieldErrors);

        VendorErrorResponse response = exceptionMapper.onInvalidRequestError(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.INVALID_PARAMETERS.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INVALID_PARAMETERS.getCode()));
    }

    @Test
    @DisplayName("onInvalidRequestError: Maps 'Invalid VendorPlayerUsername' message to USER_NOT_FOUND (310)")
    void onInvalidRequestError_InvalidVendorPlayerUsername_ReturnsUserNotFound() {
        InvalidRequestException ex = new InvalidRequestException("Invalid VendorPlayerUsername");

        VendorErrorResponse response = exceptionMapper.onInvalidRequestError(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.USER_NOT_FOUND.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.USER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("onInvalidRequestError: Maps default field errors to INVALID_PARAMETERS (610)")
    void onInvalidRequestError_DefaultFieldError_ReturnsInvalidParameters() {
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("userId", "must not be blank");
        InvalidRequestException ex = new InvalidRequestException("userId must not be blank", fieldErrors);

        VendorErrorResponse response = exceptionMapper.onInvalidRequestError(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.INVALID_PARAMETERS.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INVALID_PARAMETERS.getCode()));
    }

    @Test
    @DisplayName("onInternalError: Maps InvalidPlayerException cause to USER_NOT_FOUND (310)")
    void onInternalError_InvalidPlayerException_ReturnsUserNotFound() {
        InvalidPlayerException cause = new InvalidPlayerException("Player account disabled");
        InternalServerException ex = new InternalServerException("Internal error", cause);

        VendorErrorResponse response = exceptionMapper.onInternalError(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.USER_NOT_FOUND.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.USER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("onInternalError: Maps generic InternalServerException to INTERNAL_ERROR_ES (399)")
    void onInternalError_GenericException_ReturnsInternalErrorEs() {
        InternalServerException ex = new InternalServerException("Unexpected server error");

        VendorErrorResponse response = exceptionMapper.onInternalError(ex);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(ResponseCodes.INTERNAL_ERROR_ES.getHttpStatus());

        ErrorResponse payload = (ErrorResponse) response.getBody();
        assertThat(payload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(payload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INTERNAL_ERROR_ES.getCode()));
    }
}
