package com.nextgen.gameaggregator.game.launcher.endorphina;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.exception.GameLaunchException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import com.nextgen.gameaggregator.game.launcher.endorphina.util.VendorUtil;
import com.nextgen.gameaggregator.vendor.endorphina.constant.Credentials;
import com.nextgen.gameaggregator.vendor.endorphina.constant.EndPoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        launcher = new EndorphinaGameLauncher(credentialUtils);
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
    @DisplayName("buildRequestBody should include 'lang=en' in sorted params, concatenate values in signature payload, and compute sign")
    void buildRequestBody_Success() {
        // Arrange
        Map<String, VendorLineCredential> credentialMap = new HashMap<>();
        VendorCredentialAccessor accessor = mock(VendorCredentialAccessor.class);

        when(context.getVendorCredentials()).thenReturn(credentialMap);
        when(credentialUtils.of(credentialMap)).thenReturn(accessor);

        when(accessor.getValue(Credentials.NODE_ID)).thenReturn("node_123");
        when(accessor.getValue(Credentials.SALT)).thenReturn("secret_salt");

        when(context.getToken()).thenReturn("token-123-abc");
        when(context.getLobbyUrl()).thenReturn("https://lobby.com");
        when(context.getVendorLanguageCode()).thenReturn("en");
        when(context.getVendorToken()).thenReturn("token123abc");

        // Act
        GameLaunchRequest request = launcher.buildRequestBody(context);

        // Assert basic request properties
        assertNotNull(request);
        assertEquals("https://lobby.com", request.getExit());
        assertEquals("en", request.getLang());
        assertEquals("node_123", request.getNodeId());
        assertEquals("token123abc", request.getToken());

        // Verify side effect of setVendorToken with dash removed
        verify(context).setVendorToken("token123abc");

        // Verify parameter map sorting
        Map<String, String> sortedParams = VendorUtil.buildSortedParams(
                "https://lobby.com", "en", "token123abc", "node_123"
        );

        assertTrue(sortedParams.containsKey("lang"));
        assertEquals("en", sortedParams.get("lang"));

        // Verify exact concatenated value-only payload string format (sorted keys: exit, lang, nodeId, token + salt)
        String queryParamsSignature = VendorUtil.getSignature(sortedParams, "secret_salt");
        String expectedPayload = "https://lobby.comennode_123token123abcsecret_salt";
        assertEquals(expectedPayload, queryParamsSignature);

        // Assert signature field exists and is non-blank
        assertNotNull(request.getSign());
        assertFalse(request.getSign().isBlank());
    }

    @Test
    @DisplayName("buildRequestBody should default null vendorLanguageCode to 'en' in request, sorted params, and signature payload")
    void buildRequestBody_WhenLanguageCodeIsNull_ShouldFallbackToEn() {
        // Arrange
        Map<String, VendorLineCredential> credentialMap = new HashMap<>();
        VendorCredentialAccessor accessor = mock(VendorCredentialAccessor.class);

        when(context.getVendorCredentials()).thenReturn(credentialMap);
        when(credentialUtils.of(credentialMap)).thenReturn(accessor);

        when(accessor.getValue(Credentials.NODE_ID)).thenReturn("node_123");
        when(accessor.getValue(Credentials.SALT)).thenReturn("secret_salt");

        when(context.getToken()).thenReturn("token-123-abc");
        when(context.getLobbyUrl()).thenReturn("https://lobby.com");
        when(context.getVendorLanguageCode()).thenReturn(null); // Explicit null
        when(context.getVendorToken()).thenReturn("token123abc");

        // Act
        GameLaunchRequest request = launcher.buildRequestBody(context);

        // Assert language fallback in request body
        assertNotNull(request);
        assertEquals("en", request.getLang());

        // Assert language fallback in VendorUtil.buildSortedParams
        Map<String, String> sortedParams = VendorUtil.buildSortedParams(
                "https://lobby.com", null, "token123abc", "node_123"
        );
        assertEquals("en", sortedParams.get("lang"));

        // Assert payload calculation uses 'en' instead of null or empty string
        String signaturePayload = VendorUtil.getSignature(sortedParams, "secret_salt");
        String expectedPayload = "https://lobby.comennode_123token123abcsecret_salt";
        assertEquals(expectedPayload, signaturePayload);
    }

    @Test
    @DisplayName("buildRequestBody should default blank vendorLanguageCode to 'en' in request, sorted params, and signature payload")
    void buildRequestBody_WhenLanguageCodeIsBlank_ShouldFallbackToEn() {
        // Arrange
        Map<String, VendorLineCredential> credentialMap = new HashMap<>();
        VendorCredentialAccessor accessor = mock(VendorCredentialAccessor.class);

        when(context.getVendorCredentials()).thenReturn(credentialMap);
        when(credentialUtils.of(credentialMap)).thenReturn(accessor);

        when(accessor.getValue(Credentials.NODE_ID)).thenReturn("node_123");
        when(accessor.getValue(Credentials.SALT)).thenReturn("secret_salt");

        when(context.getToken()).thenReturn("token-123-abc");
        when(context.getLobbyUrl()).thenReturn("https://lobby.com");
        when(context.getVendorLanguageCode()).thenReturn("   "); // Explicit blank string
        when(context.getVendorToken()).thenReturn("token123abc");

        // Act
        GameLaunchRequest request = launcher.buildRequestBody(context);

        // Assert language fallback in request body
        assertNotNull(request);
        assertEquals("en", request.getLang());

        // Assert language fallback in VendorUtil.buildSortedParams
        Map<String, String> sortedParams = VendorUtil.buildSortedParams(
                "https://lobby.com", "   ", "token123abc", "node_123"
        );
        assertEquals("en", sortedParams.get("lang"));

        // Assert payload calculation uses 'en' instead of whitespace/empty string
        String signaturePayload = VendorUtil.getSignature(sortedParams, "secret_salt");
        String expectedPayload = "https://lobby.comennode_123token123abcsecret_salt";
        assertEquals(expectedPayload, signaturePayload);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n"})
    @DisplayName("VendorUtil.buildSortedParams should substitute null or blank language code with 'en'")
    void buildSortedParams_WhenLangIsNullOptionallyOrBlank_ShouldDefaultToEn(String inputLang) {
        Map<String, String> params = VendorUtil.buildSortedParams("https://lobby.com", inputLang, "token123", "node_1");

        assertEquals("en", params.get("lang"));
    }

    @Test
    @DisplayName("buildRequestBody should wrap exception thrown inside try-block into GameLaunchException")
    void buildRequestBody_WhenExceptionOccursInTryBlock_ShouldThrowGameLaunchException() {
        // Arrange
        Map<String, VendorLineCredential> credentialMap = new HashMap<>();
        VendorCredentialAccessor accessor = mock(VendorCredentialAccessor.class);

        when(context.getVendorCredentials()).thenReturn(credentialMap);
        when(credentialUtils.of(credentialMap)).thenReturn(accessor);

        when(accessor.getValue(Credentials.NODE_ID)).thenReturn("node_123");
        when(accessor.getValue(Credentials.SALT)).thenReturn("secret_salt");

        when(context.getToken()).thenReturn("token-123");
        // First invocation succeeds outside try block, second invocation inside try block throws
        when(context.getLobbyUrl())
                .thenReturn("https://lobby.com")
                .thenThrow(new RuntimeException("Error accessing lobby URL during request building"));

        when(context.getVendorLanguageCode()).thenReturn("en");
        when(context.getVendorToken()).thenReturn("token123");

        // Act & Assert
        GameLaunchException exception = assertThrows(
                GameLaunchException.class,
                () -> launcher.buildRequestBody(context)
        );

        assertEquals("Error accessing lobby URL during request building", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }
}