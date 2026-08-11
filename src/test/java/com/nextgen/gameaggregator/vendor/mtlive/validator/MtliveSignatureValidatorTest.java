package com.nextgen.gameaggregator.vendor.mtlive.validator;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mtlive.config.MtliveConfig;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.util.VendorUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MtliveSignatureValidatorTest {

    @Mock
    private VendorPlayerDataService vendorPlayerDataService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private VendorCredentialAccessor credentialAccessor;

    @Mock
    private VendorLineService vendorLineService;

    @Spy
    @InjectMocks
    private MtliveSignatureValidator validator;

    private Map<String, String> formFields;
    private static final String VALID_USER_ID = "player123";
    private static final Integer VENDOR_LINE_ID = 100;
    private static final String CLIENT_ID = "client_abc";
    private static final String CLIENT_SECRET = "secret_xyz";
    private static final String TIMESTAMP = "1700000000";
    private static final String MSG_PAYLOAD = "test_msg_payload";

    @BeforeEach
    void setUp() {
        formFields = new HashMap<>();
        formFields.put("user_id", VALID_USER_ID);
        formFields.put("msg", MSG_PAYLOAD);

        lenient().when(request.getContentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        lenient().when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    }

    @AfterEach
    void tearDown() {
        // Error-path resolvers read the current request via RequestContextHolder; clear it so a
        // test that binds one cannot leak into another (e.g. the all-strategies-fail case).
        RequestContextHolder.resetRequestAttributes();
    }

    private VendorLineCredential credential(String name, String value) {
        VendorLineCredential c = new VendorLineCredential();
        c.setName(name);
        c.setValue(value);
        return c;
    }

    private Map<String, VendorLineCredential> validCredsMap() {
        Map<String, VendorLineCredential> creds = new HashMap<>();
        creds.put(Credentials.CLIENT_SECRET, credential(Credentials.CLIENT_SECRET, "testSecret"));
        creds.put(Credentials.CLIENT_ID, credential(Credentials.CLIENT_ID, "testClientId"));
        creds.put(Credentials.DES_KEY, credential(Credentials.DES_KEY, "12345678")); // 8 bytes for DES
        creds.put(Credentials.DES_IV, credential(Credentials.DES_IV, "87654321"));   // 8 bytes for DES
        return creds;
    }

    private void mockSecurityHeadersAndCredentials(String signature) {
        when(request.getHeader(Headers.API_SI)).thenReturn(signature);
        when(request.getHeader(Headers.API_CI)).thenReturn(CLIENT_ID);
        when(request.getHeader(Headers.API_TS)).thenReturn(TIMESTAMP);

        doReturn(credentialAccessor)
                .when(validator)
                .getCredentialAccessorByKeyValue(eq(MtliveConfig.ID), eq(Credentials.CLIENT_ID), eq(CLIENT_ID));
        // The validator reads the client secret off the resolved accessor to rebuild the
        // signing key; without this stub validateHeadersAndSignature() short-circuits with
        // "Missing Credentials clientSecret" and no test ever reaches its target assertion.
        when(credentialAccessor.getValue(Credentials.CLIENT_SECRET)).thenReturn(CLIENT_SECRET);
    }

    private String calculateMD5Reverse(String msg, String key) {
        // MD5_REVERSE == Md5SignatureStrategy(SECRET_PAYLOAD) -> md5(secret + payload),
        // where here secret == timestamp+clientSecret+clientId (the "key") and payload == msg.
        return DigestUtils.md5Hex(key + msg);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("validate should throw SignatureValidationException when user_id is missing or blank")
    void validate_MissingOrBlankUserId_ThrowsSignatureValidationException(String invalidUserId) {
        if (invalidUserId == null) {
            formFields.remove("user_id");
        } else {
            formFields.put("user_id", invalidUserId);
        }

        String validSig = calculateMD5Reverse(MSG_PAYLOAD, TIMESTAMP + CLIENT_SECRET + CLIENT_ID);
        mockSecurityHeadersAndCredentials(validSig);

        SignatureValidationException exception = assertThrows(
                SignatureValidationException.class,
                () -> validator.validate(request, formFields, "")
        );

        assertEquals("INVALID_PARAMETER", exception.getMessage());
        verify(vendorPlayerDataService, never()).getByUsername(any());
    }

    @Test
    @DisplayName("validate should throw SignatureValidationException with PlayerNotFoundException cause when player is unknown")
    void validate_UnknownPlayer_ThrowsSignatureValidationException() {
        String validSig = calculateMD5Reverse(MSG_PAYLOAD, TIMESTAMP + CLIENT_SECRET + CLIENT_ID);
        mockSecurityHeadersAndCredentials(validSig);

        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VALID_USER_ID));

        SignatureValidationException exception = assertThrows(
                SignatureValidationException.class,
                () -> validator.validate(request, formFields, "")
        );

        assertEquals("INVALID_PARAMETER", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(PlayerNotFoundException.class, exception.getCause());
    }

    @Test
    @DisplayName("validate should surface a retriable server error (not INVALID_PARAMETER) when player lookup fails on a backend fault")
    void validate_PlayerLookupBackendFault_ThrowsInternalServerException() {
        String validSig = calculateMD5Reverse(MSG_PAYLOAD, TIMESTAMP + CLIENT_SECRET + CLIENT_ID);
        mockSecurityHeadersAndCredentials(validSig);

        // A transient infra fault (e.g. DB/cache down) surfaces as a generic RuntimeException,
        // NOT EntityNotFoundException. It must not be reported to the vendor as a bad request.
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new RuntimeException("connection pool exhausted"));

        assertThrows(
                InternalServerException.class,
                () -> validator.validate(request, formFields, "")
        );
    }

    @Test
    @DisplayName("validate should return successful ValidationResult when request, headers, signature, and player are valid")
    void validate_ValidCase_ReturnsSuccessResult() throws SignatureValidationException {
        String validSig = calculateMD5Reverse(MSG_PAYLOAD, TIMESTAMP + CLIENT_SECRET + CLIENT_ID);
        mockSecurityHeadersAndCredentials(validSig);

        VendorPlayer mockPlayer = new VendorPlayer();
        mockPlayer.setUsername(VALID_USER_ID);
        mockPlayer.setVendorLineId(VENDOR_LINE_ID);
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID)).thenReturn(mockPlayer);

        ValidationResult result = validator.validate(request, formFields, "");

        assertNotNull(result);
        assertTrue(result.valid());
        // The decrypted form fields must be returned so the controller can bind the payload
        // (they reach the controller only via enrichRequestFields(additionalFields)).
        assertEquals(VALID_USER_ID, result.additionalFields().get("user_id"));
        assertEquals(MSG_PAYLOAD, result.additionalFields().get("msg"));
        // The resolved vendorLineId is carried as a trusted request attribute, NOT a form field
        // (so a raw-body value can never shadow it).
        verify(request).setAttribute(VendorUtil.RESOLVED_VENDOR_LINE_ATTR, VENDOR_LINE_ID);
        assertFalse(result.additionalFields().containsKey("vendor_line_id"));
        verify(vendorPlayerDataService, times(1)).getByUsername(VALID_USER_ID);
    }

    // --- error-path credential resolution chain (resolveCredentialAccessorForError) ---

    @Test
    @DisplayName("error-path resolves credentials by user_id and does not consult the X-API-CI header fallback")
    void onInvalidSignature_ResolvesByUserId() {
        VendorPlayer player = new VendorPlayer();
        player.setUsername(VALID_USER_ID);
        player.setVendorLineId(VENDOR_LINE_ID);
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID)).thenReturn(player);
        // getCredentialAccessorByVendorLineId(100) -> new VendorCredentialAccessor(mapCredentialsByName(100))
        when(vendorLineService.mapCredentialsByName(VENDOR_LINE_ID)).thenReturn(validCredsMap());

        VendorErrorResponse resp = validator.onInvalidSignature(new SignatureValidationException("INVALID_PARAMETER"), formFields);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        // user_id resolved directly; the header fallback must never be consulted.
        verify(validator, never()).getCredentialAccessorByKeyValue(any(), any(), any());
    }

    @Test
    @DisplayName("error-path falls back to the X-API-CI header when user_id cannot be resolved")
    void onPlayerNotFound_FallsBackToHeaderClientId() {
        // user_id resolution fails (player not found) ...
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VALID_USER_ID));
        // ... so the chain falls back to the mandatory X-API-CI header (read via RequestContextHolder).
        when(request.getHeader(Headers.API_CI)).thenReturn(CLIENT_ID);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        doReturn(new VendorCredentialAccessor(validCredsMap()))
                .when(validator)
                .getCredentialAccessorByKeyValue(eq(MtliveConfig.ID), eq(Credentials.CLIENT_ID), eq(CLIENT_ID));

        VendorErrorResponse resp = validator.onPlayerNotFound(new SignatureValidationException("INVALID_PARAMETER"), formFields);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        verify(vendorPlayerDataService).getByUsername(VALID_USER_ID);
        verify(validator).getCredentialAccessorByKeyValue(eq(MtliveConfig.ID), eq(Credentials.CLIENT_ID), eq(CLIENT_ID));
    }

    @Test
    @DisplayName("error-path returns encrypted BAD_REQUEST fallback when no resolution strategy succeeds")
    void onInvalidSignature_AllStrategiesFail_ReturnsBadRequest() {
        formFields.remove("user_id");            // resolveByUserId -> empty (blank guard)
        // no RequestContextHolder bound -> resolveByHeaderClientId -> empty

        VendorErrorResponse resp = validator.onInvalidSignature(new SignatureValidationException("Signature does not match"), formFields);

        assertEquals(400, resp.getStatusCode().value());
        assertEquals("Invalid signature or missing credentials", resp.getBody());
        verify(vendorPlayerDataService, never()).getByUsername(any());
    }
}