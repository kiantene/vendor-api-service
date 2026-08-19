package com.nextgen.gameaggregator.vendor.mtlive.validator;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalConfigurationException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.exception.PlayerNotFoundException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.entity.ga.VendorLine;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
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
        creds.put(Credentials.CLIENT_SECRET, credential(Credentials.CLIENT_SECRET, CLIENT_SECRET));
        creds.put(Credentials.CLIENT_ID, credential(Credentials.CLIENT_ID, CLIENT_ID));
        creds.put(Credentials.DES_KEY, credential(Credentials.DES_KEY, "12345678"));
        creds.put(Credentials.DES_IV, credential(Credentials.DES_IV, "87654321"));
        return creds;
    }

    private void mockSecurityHeadersAndCredentials(String signature) {
        when(request.getHeader(Headers.API_SI)).thenReturn(signature);
        when(request.getHeader(Headers.API_CI)).thenReturn(CLIENT_ID);
        when(request.getHeader(Headers.API_TS)).thenReturn(TIMESTAMP);

        lenient().when(vendorLineService.mapCredentialsByName(VENDOR_LINE_ID)).thenReturn(validCredsMap());
    }

    private String calculateMD5Reverse(String msg, String key) {
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

        SignatureValidationException exception = assertThrows(
                SignatureValidationException.class,
                () -> validator.validate(request, formFields, "")
        );

        assertEquals("INVALID_PARAMETER", exception.getMessage());
        verify(vendorPlayerDataService, never()).getByUsername(any());
    }

    @Test
    @DisplayName("validate: unknown player WITH a valid signature still yields PLAYER_NOT_FOUND (signal preserved for authenticated MTLive)")
    void validate_UnknownPlayer_ValidSignature_ThrowsPlayerNotFound() {
        // Player unknown -> no line; the signature is still verified via the clientId fallback.
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VALID_USER_ID));
        when(request.getHeader(Headers.API_SI))
                .thenReturn(calculateMD5Reverse(MSG_PAYLOAD, TIMESTAMP + CLIENT_SECRET + CLIENT_ID));
        when(request.getHeader(Headers.API_CI)).thenReturn(CLIENT_ID);
        when(request.getHeader(Headers.API_TS)).thenReturn(TIMESTAMP);
        // Unknown-player fallback: resolve the secret by clientId SOLELY to verify the signature.
        doReturn(new VendorCredentialAccessor(validCredsMap()))
                .when(validator)
                .getCredentialAccessorByKeyValue(eq(MtliveConfig.ID), eq(Credentials.CLIENT_ID), eq(CLIENT_ID));

        SignatureValidationException exception = assertThrows(
                SignatureValidationException.class,
                () -> validator.validate(request, formFields, "")
        );

        assertEquals("INVALID_PARAMETER", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(PlayerNotFoundException.class, exception.getCause());
    }

    @Test
    @DisplayName("validate: unknown player with an INVALID signature fails as a signature error, NOT PLAYER_NOT_FOUND (no enumeration oracle)")
    void validate_UnknownPlayer_InvalidSignature_DoesNotRevealPlayerNotFound() {
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VALID_USER_ID));
        when(request.getHeader(Headers.API_SI)).thenReturn("wrong-signature");
        when(request.getHeader(Headers.API_CI)).thenReturn(CLIENT_ID);
        when(request.getHeader(Headers.API_TS)).thenReturn(TIMESTAMP);
        doReturn(new VendorCredentialAccessor(validCredsMap()))
                .when(validator)
                .getCredentialAccessorByKeyValue(eq(MtliveConfig.ID), eq(Credentials.CLIENT_ID), eq(CLIENT_ID));

        SignatureValidationException exception = assertThrows(
                SignatureValidationException.class,
                () -> validator.validate(request, formFields, "")
        );

        // The signature check fails first, so an unknown player is indistinguishable from a known
        // player with a bad signature -> no existence leak. The cause must NOT be PlayerNotFound.
        assertEquals("Signature does not match", exception.getMessage());
        assertFalse(exception.getCause() instanceof PlayerNotFoundException);
    }

    @Test
    @DisplayName("validate: unknown player + unknown clientId surfaces a generic signature error, NOT a raw InternalConfigurationException (no clientId oracle)")
    void validate_UnknownPlayer_UnknownClientId_ThrowsSignatureValidationException() {
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VALID_USER_ID));
        when(request.getHeader(Headers.API_SI)).thenReturn("any-signature");
        when(request.getHeader(Headers.API_CI)).thenReturn(CLIENT_ID);
        when(request.getHeader(Headers.API_TS)).thenReturn(TIMESTAMP);
        // Unknown clientId: the fallback lookup fails as InternalConfigurationException (no MTLive line).
        doThrow(new InternalConfigurationException("clientID not found"))
                .when(validator)
                .getCredentialAccessorByKeyValue(eq(MtliveConfig.ID), eq(Credentials.CLIENT_ID), eq(CLIENT_ID));

        // It must be converted to a generic signature error (indistinguishable from a bad signature),
        // NOT bubble out as InternalConfigurationException -> raw 500 (which would leak clientId validity).
        SignatureValidationException exception = assertThrows(
                SignatureValidationException.class,
                () -> validator.validate(request, formFields, "")
        );

        assertEquals("Signature does not match", exception.getMessage());
        assertFalse(exception.getCause() instanceof PlayerNotFoundException);
    }

    @Test
    @DisplayName("validate should surface a retriable server error (not INVALID_PARAMETER) when player lookup fails on a backend fault")
    void validate_PlayerLookupBackendFault_ThrowsInternalServerException() {
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new RuntimeException("connection pool exhausted"));

        assertThrows(
                InternalServerException.class,
                () -> validator.validate(request, formFields, "")
        );
    }

    @Test
    @DisplayName("validate should throw InternalConfigurationException when credentials cannot be resolved for vendorLineId")
    void validate_MissingCredentials_ThrowsInternalConfigurationException() {
        // 1. Mock required security headers so initial header validation succeeds
        String validSig = calculateMD5Reverse(MSG_PAYLOAD, TIMESTAMP + CLIENT_SECRET + CLIENT_ID);
        when(request.getHeader(Headers.API_SI)).thenReturn(validSig);
        when(request.getHeader(Headers.API_CI)).thenReturn(CLIENT_ID);
        when(request.getHeader(Headers.API_TS)).thenReturn(TIMESTAMP);

        // 2. Mock player lookup returning a valid player with vendorLineId
        VendorPlayer mockPlayer = new VendorPlayer();
        mockPlayer.setUsername(VALID_USER_ID);
        mockPlayer.setVendorLineId(VENDOR_LINE_ID);
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID)).thenReturn(mockPlayer);

        // 3. Provide CLIENT_ID so header validation passes, but omit CLIENT_SECRET to trigger InternalConfigurationException
        Map<String, VendorLineCredential> incompleteCredsMap = new HashMap<>();
        incompleteCredsMap.put(Credentials.CLIENT_ID, credential(Credentials.CLIENT_ID, CLIENT_ID));
        when(vendorLineService.mapCredentialsByName(VENDOR_LINE_ID)).thenReturn(incompleteCredsMap);

        InternalConfigurationException exception = assertThrows(
                InternalConfigurationException.class,
                () -> validator.validate(request, formFields, "")
        );

        assertTrue(exception.getMessage().contains(Credentials.CLIENT_SECRET));
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
        assertEquals(VALID_USER_ID, result.additionalFields().get("user_id"));
        assertEquals(MSG_PAYLOAD, result.additionalFields().get("msg"));
        verify(request).setAttribute(VendorUtil.RESOLVED_VENDOR_LINE_ATTR, VENDOR_LINE_ID);
        assertFalse(result.additionalFields().containsKey("vendor_line_id"));
        verify(vendorPlayerDataService, times(1)).getByUsername(VALID_USER_ID);
    }

    // --- error-path credential resolution chain ---

    @Test
    @DisplayName("error-path resolves credentials by user_id and does not consult the X-API-CI header fallback")
    void onInvalidSignature_ResolvesByUserId() {
        VendorPlayer player = new VendorPlayer();
        player.setUsername(VALID_USER_ID);
        player.setVendorLineId(VENDOR_LINE_ID);
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID)).thenReturn(player);
        when(vendorLineService.mapCredentialsByName(VENDOR_LINE_ID)).thenReturn(validCredsMap());

        VendorErrorResponse resp = validator.onInvalidSignature(new SignatureValidationException("INVALID_PARAMETER"), formFields);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        verify(validator, never()).getCredentialAccessorByKeyValue(any(), any(), any());
    }

    @Test
    @DisplayName("error-path falls back to the X-API-CI header when user_id cannot be resolved")
    void onPlayerNotFound_FallsBackToHeaderClientId() {
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VALID_USER_ID));
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
    @DisplayName("onPlayerNotFound returns encrypted BAD_REQUEST fallback when no resolution strategy succeeds")
    void onPlayerNotFound_AllStrategiesFail_ReturnsBadRequest() {
        formFields.remove("user_id");

        VendorErrorResponse resp = validator.onPlayerNotFound(new SignatureValidationException("INVALID_PARAMETER"), formFields);

        assertEquals(400, resp.getStatusCode().value());
        assertEquals("Player not found and credentials unavailable", resp.getBody());
        verify(vendorPlayerDataService, never()).getByUsername(any());
    }

    @Test
    @DisplayName("error-path returns encrypted BAD_REQUEST fallback when no resolution strategy succeeds")
    void onInvalidSignature_AllStrategiesFail_ReturnsBadRequest() {
        formFields.remove("user_id");

        VendorErrorResponse resp = validator.onInvalidSignature(new SignatureValidationException("Signature does not match"), formFields);

        assertEquals(400, resp.getStatusCode().value());
        assertEquals("Invalid signature or missing credentials", resp.getBody());
        verify(vendorPlayerDataService, never()).getByUsername(any());
    }

    // --- getCredentialAccessorByKeyValue tests ---

    @Test
    @DisplayName("getCredentialAccessorByKeyValue returns accessor when vendor line exists and belongs to expected vendor")
    void getCredentialAccessorByKeyValue_Success() throws CredentialNotFoundException {
        VendorLine vendorLine = new VendorLine();
        vendorLine.setId(VENDOR_LINE_ID);
        vendorLine.setVendorId(MtliveConfig.ID);

        when(vendorLineService.getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID))
                .thenReturn(VENDOR_LINE_ID);
        when(vendorLineService.getVendorLine(VENDOR_LINE_ID))
                .thenReturn(vendorLine);
        when(vendorLineService.mapCredentialsByName(VENDOR_LINE_ID))
                .thenReturn(validCredsMap());

        VendorCredentialAccessor accessor = validator.getCredentialAccessorByKeyValue(
                MtliveConfig.ID, Credentials.CLIENT_ID, CLIENT_ID);

        assertNotNull(accessor);
        assertEquals(CLIENT_SECRET, accessor.getValue(Credentials.CLIENT_SECRET));
        assertEquals(CLIENT_ID, accessor.getValue(Credentials.CLIENT_ID));
        verify(vendorLineService).getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID);
        verify(vendorLineService).getVendorLine(VENDOR_LINE_ID);
        verify(vendorLineService).mapCredentialsByName(VENDOR_LINE_ID);
    }

    @Test
    @DisplayName("getCredentialAccessorByKeyValue throws InternalConfigurationException when credential is not found")
    void getCredentialAccessorByKeyValue_CredentialNotFound_ThrowsInternalConfigurationException() throws CredentialNotFoundException {
        when(vendorLineService.getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID))
                .thenThrow(new CredentialNotFoundException("Not found"));

        InternalConfigurationException exception = assertThrows(
                InternalConfigurationException.class,
                () -> validator.getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, CLIENT_ID)
        );

        assertTrue(exception.getMessage().contains(Credentials.CLIENT_ID + " not found"));
        verify(vendorLineService).getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID);
        verify(vendorLineService, never()).getVendorLine(any());
    }

    @Test
    @DisplayName("getCredentialAccessorByKeyValue throws InternalConfigurationException when resolved line belongs to another vendor")
    void getCredentialAccessorByKeyValue_VendorMismatch_ThrowsInternalConfigurationException() throws CredentialNotFoundException {
        Integer otherVendorId = 999;
        VendorLine otherVendorLine = new VendorLine();
        otherVendorLine.setId(VENDOR_LINE_ID);
        otherVendorLine.setVendorId(otherVendorId);

        when(vendorLineService.getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID))
                .thenReturn(VENDOR_LINE_ID);
        when(vendorLineService.getVendorLine(VENDOR_LINE_ID))
                .thenReturn(otherVendorLine);

        InternalConfigurationException exception = assertThrows(
                InternalConfigurationException.class,
                () -> validator.getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, CLIENT_ID)
        );

        assertEquals(
                String.format("Credential %s=%s resolved to line %d belonging to vendorId %d, expected vendorId %d",
                        Credentials.CLIENT_ID, CLIENT_ID, VENDOR_LINE_ID, otherVendorId, MtliveConfig.ID),
                exception.getMessage()
        );
        verify(vendorLineService).getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID);
        verify(vendorLineService).getVendorLine(VENDOR_LINE_ID);
        verify(vendorLineService, never()).mapCredentialsByName(any());
    }

    @Test
    @DisplayName("getCredentialAccessorByKeyValue throws InternalConfigurationException when vendor line does not exist")
    void getCredentialAccessorByKeyValue_NullVendorLine_ThrowsInternalConfigurationException() throws CredentialNotFoundException {
        when(vendorLineService.getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID))
                .thenReturn(VENDOR_LINE_ID);
        when(vendorLineService.getVendorLine(VENDOR_LINE_ID))
                .thenReturn(null);

        InternalConfigurationException exception = assertThrows(
                InternalConfigurationException.class,
                () -> validator.getCredentialAccessorByKeyValue(MtliveConfig.ID, Credentials.CLIENT_ID, CLIENT_ID)
        );

        assertEquals(
                String.format("Vendor line %d not found for credential %s=%s", VENDOR_LINE_ID, Credentials.CLIENT_ID, CLIENT_ID),
                exception.getMessage()
        );
        verify(vendorLineService).getVendorLineIdListByNameAndValue(Credentials.CLIENT_ID, CLIENT_ID);
        verify(vendorLineService).getVendorLine(VENDOR_LINE_ID);
        verify(vendorLineService, never()).mapCredentialsByName(any());
    }

    @Test
    @DisplayName("validate should throw SignatureValidationException when request X-API-CI header mismatches player line credentials")
    void validate_MismatchedClientIdHeader_ThrowsSignatureValidationException() {
        String requestHeaderClientId = "op-A-clientId";
        String lineCredentialClientId = "op-B-clientId";

        // 1. Mock request with Header X-API-CI = op-A-clientId
        String validSig = calculateMD5Reverse(MSG_PAYLOAD, TIMESTAMP + CLIENT_SECRET + requestHeaderClientId);
        when(request.getHeader(Headers.API_SI)).thenReturn(validSig);
        when(request.getHeader(Headers.API_CI)).thenReturn(requestHeaderClientId);
        when(request.getHeader(Headers.API_TS)).thenReturn(TIMESTAMP);

        // 2. Mock resolved player with valid vendorLineId
        VendorPlayer mockPlayer = new VendorPlayer();
        mockPlayer.setUsername(VALID_USER_ID);
        mockPlayer.setVendorLineId(VENDOR_LINE_ID);
        when(vendorPlayerDataService.getByUsername(VALID_USER_ID)).thenReturn(mockPlayer);

        // 3. Mock player line credentials where CLIENT_ID = op-B-clientId
        Map<String, VendorLineCredential> playerLineCredsMap = new HashMap<>();
        playerLineCredsMap.put(Credentials.CLIENT_SECRET, credential(Credentials.CLIENT_SECRET, CLIENT_SECRET));
        playerLineCredsMap.put(Credentials.CLIENT_ID, credential(Credentials.CLIENT_ID, lineCredentialClientId));
        when(vendorLineService.mapCredentialsByName(VENDOR_LINE_ID)).thenReturn(playerLineCredsMap);

        // 4. Validate that the mismatch guard triggers and throws expected exception
        SignatureValidationException exception = assertThrows(
                SignatureValidationException.class,
                () -> validator.validate(request, formFields, "")
        );

        assertEquals("Header X-API-CI does not match player vendor line credentials", exception.getMessage());
        verify(vendorPlayerDataService, times(1)).getByUsername(VALID_USER_ID);
        verify(vendorLineService, times(1)).mapCredentialsByName(VENDOR_LINE_ID);
    }
}
