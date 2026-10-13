package com.nextgen.gameaggregator.vendor.megafair.validator;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.megafair.constant.Credentials;
import com.nextgen.gameaggregator.vendor.megafair.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.megafair.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.megafair.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.megafair.util.VendorUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MegafairSignatureValidatorTest {

    @Mock
    private VendorPlayerDataService vendorPlayerDataService;

    @Mock
    private VendorLineService vendorLineService;

    @Mock
    private GameSessionDataService gameSessionDataService;

    @Mock
    private VendorUtil vendorUtil;

    @Mock
    private HttpServletRequest request;

    @Captor
    private ArgumentCaptor<ErrorResponse> errorResponseCaptor;

    private MegafairSignatureValidator validator;
    private MockedStatic<LogContextHolder> logContextHolderMocked;
    private LogContext logContext;

    @BeforeEach
    void setUp() {
        validator = new MegafairSignatureValidator(
                vendorPlayerDataService,
                vendorLineService,
                gameSessionDataService,
                vendorUtil
        );

        logContext = mock(LogContext.class);
        logContextHolderMocked = mockStatic(LogContextHolder.class);
        logContextHolderMocked.when(LogContextHolder::get).thenReturn(logContext);
    }

    @AfterEach
    void tearDown() {
        logContextHolderMocked.close();
    }

    @Test
    @DisplayName("validate: Successful signature validation for /betresult endpoint")
    void validate_Success() throws Exception {
        String secretKey = "mySecret";
        String stringToSign = "testUser50|tx10r123g456s789";
        String validHash = DigestUtils.md5Hex(stringToSign + secretKey);

        Map<String, String> formFields = new HashMap<>();
        formFields.put("userId", "testUser");
        formFields.put("bet", "50|tx1");
        formFields.put("isRoundFinished", "0");
        formFields.put("roundId", "r123");
        formFields.put("gameId", "g456");
        formFields.put("gameSessionId", "s789");
        formFields.put("hash", validHash);

        VendorPlayer mockVendorPlayer = mock(VendorPlayer.class);
        when(mockVendorPlayer.getVendorLineId()).thenReturn(100);

        when(request.getRequestURI()).thenReturn(EndPoints.PATH + EndPoints.BETRESULT);
        when(request.getQueryString()).thenReturn("userId=testUser&bet=50|tx1");
        when(vendorPlayerDataService.getByUsername("testUser")).thenReturn(mockVendorPlayer);
        when(vendorLineService.getCredentialValueByName(100, Credentials.SECRET)).thenReturn(secretKey);

        validator.validate(request, formFields, null);

        verify(vendorPlayerDataService).getByUsername("testUser");
        verify(vendorLineService).getCredentialValueByName(100, Credentials.SECRET);
        verify(logContext).setBody("userId=testUser&bet=50|tx1");
    }

    @Test
    @DisplayName("validate: Successful signature validation for /authenticate endpoint using token")
    void validate_Authenticate_Success() throws Exception {
        String secretKey = "mySecret";
        String stringToSign = "myToken123s789";
        String validHash = DigestUtils.md5Hex(stringToSign + secretKey);

        Map<String, String> formFields = new HashMap<>();
        formFields.put("token", "myToken123");
        formFields.put("gameSessionId", "s789");
        formFields.put("hash", validHash);

        GameSession mockGameSession = mock(GameSession.class);
        when(mockGameSession.getVendorPlayerUsername()).thenReturn("testUser");

        VendorPlayer mockVendorPlayer = mock(VendorPlayer.class);
        when(mockVendorPlayer.getVendorLineId()).thenReturn(100);

        when(request.getRequestURI()).thenReturn(EndPoints.PATH + EndPoints.AUTHENTICATE);
        when(request.getQueryString()).thenReturn("token=myToken123&gameSessionId=s789");
        when(gameSessionDataService.getByToken("myToken123", null)).thenReturn(mockGameSession);
        when(vendorPlayerDataService.getByUsername("testUser")).thenReturn(mockVendorPlayer);
        when(vendorLineService.getCredentialValueByName(100, Credentials.SECRET)).thenReturn(secretKey);

        validator.validate(request, formFields, null);

        verify(gameSessionDataService).getByToken("myToken123", null);
        verify(vendorPlayerDataService).getByUsername("testUser");
        verify(vendorLineService).getCredentialValueByName(100, Credentials.SECRET);
    }

    @Test
    @DisplayName("validate: Throws SignatureValidationException when hash is null or empty on supported endpoint")
    void validate_NullOrBlankHash_ThrowsException() {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("userId", "testUser");
        formFields.put("hash", "");

        when(request.getRequestURI()).thenReturn(EndPoints.PATH + EndPoints.BETRESULT);
        when(request.getQueryString()).thenReturn("userId=testUser");

        assertThatThrownBy(() -> validator.validate(request, formFields, null))
                .isInstanceOf(SignatureValidationException.class)
                .hasMessageContaining("Hash is null or empty");
    }

    @Test
    @DisplayName("validate: Throws SignatureValidationException when passKey is null or empty")
    void validate_MissingPassKey_ThrowsException() throws CredentialNotFoundException {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("userId", "unknownUser");
        formFields.put("hash", "irrelevant");

        VendorPlayer mockVendorPlayer = mock(VendorPlayer.class);
        when(mockVendorPlayer.getVendorLineId()).thenReturn(100);

        when(request.getRequestURI()).thenReturn(EndPoints.PATH + EndPoints.BETRESULT);
        when(request.getQueryString()).thenReturn("userId=unknownUser");
        when(vendorPlayerDataService.getByUsername("unknownUser")).thenReturn(mockVendorPlayer);
        when(vendorLineService.getCredentialValueByName(100, Credentials.SECRET)).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(request, formFields, null))
                .isInstanceOf(SignatureValidationException.class)
                .hasMessageContaining("Missing Credentials passKey");
    }

    @Test
    @DisplayName("validate: Throws SignatureValidationException wrapping EntityNotFoundException when player is not found")
    void validate_PlayerNotFound_ThrowsException() {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("userId", "unknownUser");
        formFields.put("hash", "irrelevant");

        when(request.getRequestURI()).thenReturn(EndPoints.PATH + EndPoints.BETRESULT);
        when(request.getQueryString()).thenReturn("userId=unknownUser");
        when(vendorPlayerDataService.getByUsername("unknownUser"))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", "unknownUser"));

        assertThatThrownBy(() -> validator.validate(request, formFields, null))
                .isInstanceOf(SignatureValidationException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    @DisplayName("validate: Throws SignatureValidationException when authenticate session is expired")
    void validate_AuthenticateExpiredSession_ThrowsException() {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("token", "expiredToken");
        formFields.put("hash", "irrelevant");

        when(request.getRequestURI()).thenReturn(EndPoints.PATH + EndPoints.AUTHENTICATE);
        when(request.getQueryString()).thenReturn("token=expiredToken");
        when(gameSessionDataService.getByToken("expiredToken", null))
                .thenThrow(new GameSessionExpiredException());

        assertThatThrownBy(() -> validator.validate(request, formFields, null))
                .isInstanceOf(SignatureValidationException.class)
                .hasMessageContaining("Invalid session context or expired token");
    }

    @Test
    @DisplayName("validate: Throws SignatureValidationException for unsupported endpoint URI")
    void validate_UnsupportedEndpoint_ThrowsException() {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("userId", "testUser");

        when(request.getRequestURI()).thenReturn("/megafair/unsupported");
        when(request.getQueryString()).thenReturn("userId=testUser");

        assertThatThrownBy(() -> validator.validate(request, formFields, null))
                .isInstanceOf(SignatureValidationException.class)
                .hasMessageContaining("Unsupported endpoint for signature validation");
    }

    @Test
    @DisplayName("getResponseCode: Maps to INVALID_TOKEN (400) when cause is GameSessionExpiredException")
    void getResponseCode_ExpiredTokenCause_ReturnsInvalidToken400() {
        Map<String, String> formFields = Map.of("token", "expired_token");
        GameSessionExpiredException expiredCause = new GameSessionExpiredException();
        SignatureValidationException ex = new SignatureValidationException("Invalid session context or expired token", expiredCause);

        VendorErrorResponse mockResponse = mock(VendorErrorResponse.class);
        when(vendorUtil.toXmlVendorResponse(eq(ResponseCodes.INVALID_TOKEN.getHttpStatus()), any(ErrorResponse.class)))
                .thenReturn(mockResponse);

        VendorErrorResponse response = validator.onInvalidSignature(ex, formFields);

        assertThat(response).isEqualTo(mockResponse);

        verify(vendorUtil).toXmlVendorResponse(
                eq(ResponseCodes.INVALID_TOKEN.getHttpStatus()),
                errorResponseCaptor.capture()
        );

        ErrorResponse capturedPayload = errorResponseCaptor.getValue();
        assertThat(capturedPayload).isNotNull();
        assertThat(capturedPayload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(capturedPayload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INVALID_TOKEN.getCode()));
    }

    @Test
    @DisplayName("onInvalidSignature: Maps to INVALID_PARAMETERS (610) when hash is null or empty")
    void onInvalidSignature_MissingHash_ReturnsInvalidParameters() {
        Map<String, String> formFields = Map.of("userId", "user123");
        SignatureValidationException ex = new SignatureValidationException("Hash is null or empty");
        VendorErrorResponse mockResponse = mock(VendorErrorResponse.class);

        when(vendorUtil.toXmlVendorResponse(eq(ResponseCodes.INVALID_PARAMETERS.getHttpStatus()), any(ErrorResponse.class)))
                .thenReturn(mockResponse);

        VendorErrorResponse response = validator.onInvalidSignature(ex, formFields);

        assertThat(response).isEqualTo(mockResponse);

        verify(vendorUtil).toXmlVendorResponse(
                eq(ResponseCodes.INVALID_PARAMETERS.getHttpStatus()),
                errorResponseCaptor.capture()
        );

        ErrorResponse capturedPayload = errorResponseCaptor.getValue();
        assertThat(capturedPayload).isNotNull();
        assertThat(capturedPayload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(capturedPayload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INVALID_PARAMETERS.getCode()));
    }

    @Test
    @DisplayName("onInvalidSignature: Maps to INVALID_PARAMETERS (610) when missing token on authenticate endpoint")
    void onInvalidSignature_MissingTokenOnAuthenticate_ReturnsInvalidParameters() {
        Map<String, String> formFields = Map.of("hash", "some_hash");
        SignatureValidationException ex = new SignatureValidationException("Token is null or empty for authenticate endpoint");
        VendorErrorResponse mockResponse = mock(VendorErrorResponse.class);

        when(vendorUtil.toXmlVendorResponse(eq(ResponseCodes.INVALID_PARAMETERS.getHttpStatus()), any(ErrorResponse.class)))
                .thenReturn(mockResponse);

        VendorErrorResponse response = validator.onInvalidSignature(ex, formFields);

        assertThat(response).isEqualTo(mockResponse);

        verify(vendorUtil).toXmlVendorResponse(
                eq(ResponseCodes.INVALID_PARAMETERS.getHttpStatus()),
                errorResponseCaptor.capture()
        );

        ErrorResponse capturedPayload = errorResponseCaptor.getValue();
        assertThat(capturedPayload).isNotNull();
        assertThat(capturedPayload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(capturedPayload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INVALID_PARAMETERS.getCode()));
    }

    @Test
    @DisplayName("onInvalidSignature: Maps to INVALID_HASH (500) on betresult/refund hash mismatch even without token")
    void onInvalidSignature_HashMismatchOnBetResultOrRefund_ReturnsInvalidHash() {
        Map<String, String> formFields = Map.of("userId", "user123", "hash", "bad_hash");
        SignatureValidationException ex = new SignatureValidationException("Calculated hash does not match provided hash");
        VendorErrorResponse mockResponse = mock(VendorErrorResponse.class);

        when(vendorUtil.toXmlVendorResponse(eq(ResponseCodes.INVALID_HASH.getHttpStatus()), any(ErrorResponse.class)))
                .thenReturn(mockResponse);

        VendorErrorResponse response = validator.onInvalidSignature(ex, formFields);

        assertThat(response).isEqualTo(mockResponse);

        verify(vendorUtil).toXmlVendorResponse(
                eq(ResponseCodes.INVALID_HASH.getHttpStatus()),
                errorResponseCaptor.capture()
        );

        ErrorResponse capturedPayload = errorResponseCaptor.getValue();
        assertThat(capturedPayload).isNotNull();
        assertThat(capturedPayload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(capturedPayload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INVALID_HASH.getCode()));
        assertThat(capturedPayload.getRequest().getUserId()).isEqualTo("user123");
    }

    @Test
    @DisplayName("onPlayerNotFound: Maps to USER_NOT_FOUND (310) when userId is present")
    void onPlayerNotFound_UserIdPresent_ReturnsUserNotFound() {
        Map<String, String> formFields = Map.of("userId", "missingUser", "hash", "some_hash");
        SignatureValidationException ex = new SignatureValidationException("Player not found");
        VendorErrorResponse mockResponse = mock(VendorErrorResponse.class);

        when(vendorUtil.toXmlVendorResponse(eq(ResponseCodes.USER_NOT_FOUND.getHttpStatus()), any(ErrorResponse.class)))
                .thenReturn(mockResponse);

        VendorErrorResponse response = validator.onPlayerNotFound(ex, formFields);

        assertThat(response).isEqualTo(mockResponse);

        verify(vendorUtil).toXmlVendorResponse(
                eq(ResponseCodes.USER_NOT_FOUND.getHttpStatus()),
                errorResponseCaptor.capture()
        );

        ErrorResponse capturedPayload = errorResponseCaptor.getValue();
        assertThat(capturedPayload).isNotNull();
        assertThat(capturedPayload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(capturedPayload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.USER_NOT_FOUND.getCode()));
        assertThat(capturedPayload.getRequest().getUserId()).isEqualTo("missingUser");
    }

    @Test
    @DisplayName("onPlayerNotFound: Maps to INVALID_PARAMETERS (610) when userId is missing or blank")
    void onPlayerNotFound_UserIdMissingOrBlank_ReturnsInvalidParameters() {
        Map<String, String> formFields = Map.of("userId", "   ", "hash", "some_hash");
        SignatureValidationException ex = new SignatureValidationException("Player not found");
        VendorErrorResponse mockResponse = mock(VendorErrorResponse.class);

        when(vendorUtil.toXmlVendorResponse(eq(ResponseCodes.INVALID_PARAMETERS.getHttpStatus()), any(ErrorResponse.class)))
                .thenReturn(mockResponse);

        VendorErrorResponse response = validator.onPlayerNotFound(ex, formFields);

        assertThat(response).isEqualTo(mockResponse);

        verify(vendorUtil).toXmlVendorResponse(
                eq(ResponseCodes.INVALID_PARAMETERS.getHttpStatus()),
                errorResponseCaptor.capture()
        );

        ErrorResponse capturedPayload = errorResponseCaptor.getValue();
        assertThat(capturedPayload).isNotNull();
        assertThat(capturedPayload.getResponse().getResult()).isEqualTo("ERROR");
        assertThat(capturedPayload.getResponse().getCode())
                .isEqualTo(String.valueOf(ResponseCodes.INVALID_PARAMETERS.getCode()));
    }
}
