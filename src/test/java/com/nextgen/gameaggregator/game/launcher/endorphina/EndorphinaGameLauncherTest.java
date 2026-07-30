package com.nextgen.gameaggregator.game.launcher.endorphina;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.game.launcher.endorphina.util.VendorUtil;
import com.nextgen.gameaggregator.vendor.endorphina.constant.Credentials;
import com.nextgen.gameaggregator.vendor.endorphina.constant.EndPoints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndorphinaGameLauncherTest {

    @Mock
    private VendorCredentialUtils credentialUtils;

    @Mock
    private GameLaunchContext context;

    private EndorphinaGameLauncher launcher;
    private MockedStatic<VendorUtil> vendorUtilMockedStatic;

    @BeforeEach
    void setUp() {
        // Instantiate real launcher without spy
        launcher = new EndorphinaGameLauncher(credentialUtils);
        vendorUtilMockedStatic = mockStatic(VendorUtil.class);
    }

    @AfterEach
    void tearDown() {
        vendorUtilMockedStatic.close();
    }

    @Test
    @DisplayName("getBaseUrl should return Endorphina base URL from vendor credentials")
    void getBaseUrl_ShouldReturnConfiguredUrl() {
        Map<String, VendorLineCredential> credentialMap = new HashMap<>();
        VendorCredentialAccessor accessor = mock(VendorCredentialAccessor.class);

        when(context.getVendorCredentials()).thenReturn(credentialMap);
        when(credentialUtils.of(credentialMap)).thenReturn(accessor);
        when(accessor.getValue(Credentials.ENDO_URL)).thenReturn("https://api.endorphina.com");

        String result = launcher.getBaseUrl(context);

        assertEquals("https://api.endorphina.com", result);
        verify(accessor).getValue(Credentials.ENDO_URL);
    }

    @Test
    @DisplayName("getPath should return Endorphina launch endpoint path")
    void getPath_ShouldReturnLaunchPath() {
        String result = launcher.getPath(context);

        assertEquals(EndPoints.LAUNCH_PATH, result);
    }

    @Test
    @DisplayName("buildRequestBody should successfully generate GameLaunchRequest with valid sign")
    void buildRequestBody_Success() {
        Map<String, VendorLineCredential> credentialMap = new HashMap<>();
        VendorCredentialAccessor accessor = mock(VendorCredentialAccessor.class);

        when(context.getVendorCredentials()).thenReturn(credentialMap);
        when(credentialUtils.of(credentialMap)).thenReturn(accessor);

        when(accessor.getValue(Credentials.NODE_ID)).thenReturn("node_123");
        when(accessor.getValue(Credentials.SALT)).thenReturn("secret_salt");

        when(context.getToken()).thenReturn("token-with-dash");
        when(context.getLobbyUrl()).thenReturn("https://lobby.com");
        when(context.getVendorLanguageCode()).thenReturn("en");
        when(context.getVendorToken()).thenReturn("tokenwithoutdash");

        Map<String, String> mockSortedParams = Collections.singletonMap("exit", "https://lobby.com");

        // Mock static utility calls
        vendorUtilMockedStatic.when(() -> VendorUtil.removeDash("token-with-dash"))
                .thenReturn("tokenwithoutdash");
        vendorUtilMockedStatic.when(() -> VendorUtil.buildSortedParams("https://lobby.com", "en", "tokenwithoutdash", "node_123"))
                .thenReturn(mockSortedParams);
        vendorUtilMockedStatic.when(() -> VendorUtil.getSignature(mockSortedParams, "secret_salt"))
                .thenReturn("query_string_signature");

        // Act
        GameLaunchRequest request = launcher.buildRequestBody(context);

        // Assert
        assertNotNull(request);
        assertEquals("https://lobby.com", request.getExit());
        assertEquals("en", request.getLang());
        assertEquals("node_123", request.getNodeId());
        assertEquals("tokenwithoutdash", request.getToken());
        // Standard SHA1_HEX hash of "query_string_signature"
        assertNotNull(request.getSign());
        assertFalse(request.getSign().isBlank());

        verify(context).setVendorToken("tokenwithoutdash");
    }

    @Test
    @DisplayName("buildRequestBody should throw GameLaunchException when credential extraction fails")
    void buildRequestBody_WhenExceptionOccurs_ShouldThrowGameLaunchException() {
        Map<String, VendorLineCredential> credentialMap = new HashMap<>();
        when(context.getVendorCredentials()).thenReturn(credentialMap);
        when(credentialUtils.of(credentialMap)).thenThrow(new RuntimeException("Missing credentials"));

        GameLaunchException exception = assertThrows(
                GameLaunchException.class,
                () -> launcher.buildRequestBody(context)
        );

        assertEquals("Missing credentials", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }
}