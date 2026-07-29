package com.nextgen.gameaggregator.game.launcher.vplus;

import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.game.launcher.vplus.member.create.MemberCreateService;
import com.nextgen.gameaggregator.game.launcher.vplus.member.login.MemberLoginService;
import com.nextgen.gameaggregator.game.launcher.vplus.util.VendorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class VplusGameLauncherBuildRequestBodyTest {

    private static final String APP_ID_VALUE = "app-id-value";
    private static final String APP_SECRET_VALUE = "app-secret-value";
    private static final String VENDOR_GAME_CODE = "vplus-game-001";
    private static final String VENDOR_PLAYER_USERNAME = "player123";
    private static final String STUB_TOKEN = "stub-login-token";

    @Mock
    private VendorCredentialUtils credentialUtils;
    @Mock
    private MemberCreateService memberCreateService;
    @Mock
    private MemberLoginService memberLoginService;
    @Mock
    private VendorCredentialAccessor credentialAccessor;

    private VplusGameLauncher launcher;

    @BeforeEach
    void setUp() {
        launcher = new VplusGameLauncher(credentialUtils, memberCreateService, memberLoginService);

        lenient().when(credentialUtils.of(anyMap())).thenReturn(credentialAccessor);
        lenient().when(credentialAccessor.getValue("apiUrl")).thenReturn("https://vplus.example.com");
        lenient().when(credentialAccessor.getValue("appId")).thenReturn(APP_ID_VALUE);
        lenient().when(credentialAccessor.getValue("appSecret")).thenReturn(APP_SECRET_VALUE);
    }

    private GameLaunchContext buildContext(String vendorLanguageCode) {
        return GameLaunchContext.builder()
                .vendorPlayerUsername(VENDOR_PLAYER_USERNAME)
                .vendorGameCode(VENDOR_GAME_CODE)
                .vendorLanguageCode(vendorLanguageCode)
                .vendorCredentials(new HashMap<>())
                .build();
    }

    @Test
    void whenLangIsPresent_includedInBothSignatureAndRequestBody() {
        GameLaunchContext context = buildContext("en");

        try (MockedStatic<TokenHolder> tokenHolder = mockStatic(TokenHolder.class);
             MockedStatic<VendorUtil> vendorUtil = mockStatic(VendorUtil.class)) {

            tokenHolder.when(TokenHolder::getToken).thenReturn(STUB_TOKEN);
            vendorUtil.when(() -> VendorUtil.generateSign(anyMap())).thenReturn("query-string-stub");

            GameLaunchRequest request = launcher.buildRequestBody(context);

            // lang must appear in the request body...
            assertThat(request.getLang()).isEqualTo("en");

            // ...and the exact same value must have been included in the map that was signed.
            ArgumentCaptor<Map<String, String>> signedParamsCaptor = ArgumentCaptor.forClass(Map.class);
            vendorUtil.verify(() -> VendorUtil.generateSign(signedParamsCaptor.capture()));
            assertThat(signedParamsCaptor.getValue()).containsEntry("lang", "en");
        }
    }

    @Test
    void whenLangIsNull_omittedFromBothSignatureAndRequestBody() {
        GameLaunchContext context = buildContext(null);

        try (MockedStatic<TokenHolder> tokenHolder = mockStatic(TokenHolder.class);
             MockedStatic<VendorUtil> vendorUtil = mockStatic(VendorUtil.class)) {

            tokenHolder.when(TokenHolder::getToken).thenReturn(STUB_TOKEN);
            vendorUtil.when(() -> VendorUtil.generateSign(anyMap())).thenReturn("query-string-stub");

            GameLaunchRequest request = launcher.buildRequestBody(context);

            // This is the crux of the fix: previously `.lang(null)` would still set the field
            // (Lombok setters don't reject null), and "lang=null" would have been written into
            // the signed base string regardless. Now both sides must agree there is no lang.
            assertThat(request.getLang()).isNull();

            ArgumentCaptor<Map<String, String>> signedParamsCaptor = ArgumentCaptor.forClass(Map.class);
            vendorUtil.verify(() -> VendorUtil.generateSign(signedParamsCaptor.capture()));
            assertThat(signedParamsCaptor.getValue()).doesNotContainKey("lang");
        }
    }

    @Test
    void whenLangIsPresent_otherFieldsAreStillPopulatedCorrectly() {
        GameLaunchContext context = buildContext("en");

        try (MockedStatic<TokenHolder> tokenHolder = mockStatic(TokenHolder.class);
             MockedStatic<VendorUtil> vendorUtil = mockStatic(VendorUtil.class)) {

            tokenHolder.when(TokenHolder::getToken).thenReturn(STUB_TOKEN);
            vendorUtil.when(() -> VendorUtil.generateSign(anyMap())).thenReturn("query-string-stub");

            GameLaunchRequest request = launcher.buildRequestBody(context);

            assertThat(request.getAppId()).isEqualTo(APP_ID_VALUE);
            assertThat(request.getToken()).isEqualTo(STUB_TOKEN);
            assertThat(request.getId()).isEqualTo(VENDOR_GAME_CODE);
            assertThat(request.getSign()).isNotBlank();
            assertThat(request.getTimestamp()).isNotBlank();
        }
    }
}
