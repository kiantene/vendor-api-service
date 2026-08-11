package com.nextgen.gameaggregator.vendor.mtlive.util;

import com.nextgen.core.exception.EntityNotFoundException;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.entity.VendorPlayer;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorUtilTest {

    @Mock
    private VendorPlayerDataService vendorPlayerDataService;

    @Mock
    private VendorLineService vendorLineService;

    @Spy
    @InjectMocks
    private VendorUtil vendorUtil;

    private Object sampleResponse;
    private static final String VALID_USERNAME = "player123";
    private static final Integer VENDOR_LINE_ID = 100;

    @BeforeEach
    void setUp() {
        SuccessResponse.Data mockData = SuccessResponse.Data.builder()
                .balance(new BigDecimal("100.00"))
                .bet_sn("123456")
                .build();

        sampleResponse = SuccessResponse.builder()
                .timestamp(System.currentTimeMillis())
                .data(mockData)
                .build();
    }

    private VendorLineCredential createCredential(Integer id, String name, String value) {
        VendorLineCredential credential = new VendorLineCredential();
        credential.setId(id);
        credential.setVendorLineId(VENDOR_LINE_ID);
        credential.setVersion(1);
        credential.setName(name);
        credential.setValue(value);
        credential.setStatus(1);
        return credential;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("encryptResponse(Object, String) should throw InternalServerException when username is blank or null")
    void encryptResponse_BlankUsername_ThrowsInternalServerException(String invalidUsername) {
        InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> vendorUtil.encryptResponse(sampleResponse, invalidUsername)
        );

        assertTrue(exception.getMessage().contains("Username is required but was null or blank"));
        verify(vendorPlayerDataService, never()).getByUsername(any());
    }

    @Test
    @DisplayName("encryptResponse(Object, String) should throw InternalServerException when player is unknown")
    void encryptResponse_UnknownPlayer_ThrowsInternalServerException() {
        when(vendorPlayerDataService.getByUsername(VALID_USERNAME))
                .thenThrow(new EntityNotFoundException(VendorPlayer.class, "username", VALID_USERNAME));

        InternalServerException exception = assertThrows(
                InternalServerException.class,
                () -> vendorUtil.encryptResponse(sampleResponse, VALID_USERNAME)
        );

        assertEquals("Failed to encrypt MT Live response", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(EntityNotFoundException.class, exception.getCause());
        verify(vendorPlayerDataService, times(1)).getByUsername(VALID_USERNAME);
        verify(vendorLineService, never()).mapCredentialsByName(anyInt());
    }

    @Test
    @DisplayName("encryptResponse(Object, String) should successfully encrypt response when player exists")
    void encryptResponse_ValidPlayer_ReturnsEncryptedResponseEntity() {
        VendorPlayer mockPlayer = new VendorPlayer();
        mockPlayer.setUsername(VALID_USERNAME);
        mockPlayer.setVendorLineId(VENDOR_LINE_ID);

        Map<String, VendorLineCredential> credsMap = new HashMap<>();
        credsMap.put(Credentials.CLIENT_SECRET, createCredential(1, Credentials.CLIENT_SECRET, "testSecret"));
        credsMap.put(Credentials.CLIENT_ID, createCredential(2, Credentials.CLIENT_ID, "testClientId"));
        credsMap.put(Credentials.DES_KEY, createCredential(3, Credentials.DES_KEY, "12345678")); // 8 bytes for DES
        credsMap.put(Credentials.DES_IV, createCredential(4, Credentials.DES_IV, "87654321"));   // 8 bytes for DES

        VendorCredentialAccessor accessor = new VendorCredentialAccessor(credsMap);

        when(vendorPlayerDataService.getByUsername(VALID_USERNAME)).thenReturn(mockPlayer);
        doReturn(accessor).when(vendorUtil).getCredentialAccessorByVendorLineId(VENDOR_LINE_ID);

        ResponseEntity<String> responseEntity = vendorUtil.encryptResponse(sampleResponse, VALID_USERNAME);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getHeaders().containsKey(Headers.API_CI));
        assertTrue(responseEntity.getHeaders().containsKey(Headers.API_SI));
        assertTrue(responseEntity.getHeaders().containsKey(Headers.API_TS));
    }

    @Test
    @DisplayName("encryptResponse(Object, String) must honour the validator-set vendorLineId request attribute and NOT fall back to a player lookup (cross-operator isolation)")
    void encryptResponse_TrustedVendorLineAttr_SkipsPlayerLookup() {
        // The validator stashes the resolved vendorLineId as a trusted request attribute; the raw
        // request body cannot set a request attribute, so it can never shadow this value.
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(VendorUtil.RESOLVED_VENDOR_LINE_ATTR)).thenReturn(VENDOR_LINE_ID);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            Map<String, VendorLineCredential> credsMap = new HashMap<>();
            credsMap.put(Credentials.CLIENT_SECRET, createCredential(1, Credentials.CLIENT_SECRET, "testSecret"));
            credsMap.put(Credentials.CLIENT_ID, createCredential(2, Credentials.CLIENT_ID, "testClientId"));
            credsMap.put(Credentials.DES_KEY, createCredential(3, Credentials.DES_KEY, "12345678"));
            credsMap.put(Credentials.DES_IV, createCredential(4, Credentials.DES_IV, "87654321"));
            VendorCredentialAccessor accessor = new VendorCredentialAccessor(credsMap);
            doReturn(accessor).when(vendorUtil).getCredentialAccessorByVendorLineId(VENDOR_LINE_ID);

            ResponseEntity<String> responseEntity = vendorUtil.encryptResponse(sampleResponse, VALID_USERNAME);

            assertNotNull(responseEntity);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            // The trusted request-attribute line id was used; the player cache/DB was never consulted.
            verify(vendorUtil).getCredentialAccessorByVendorLineId(VENDOR_LINE_ID);
            verify(vendorPlayerDataService, never()).getByUsername(any());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}