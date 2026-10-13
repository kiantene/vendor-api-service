package com.nextgen.gameaggregator.game.launcher.gpkv2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.security.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class Gpkv2GameLauncherBuildRequestBodyTest {

    private static final String SIGNATURE_HEADER = "X-GPK-SIGNATURE";

    @Mock
    private VendorCredentialUtils credentialUtils;
    @Mock
    private VendorCredentialAccessor credentialAccessor;

    private Gpkv2GameLauncher launcher;

    @BeforeEach
    void setUp() {
        launcher = new Gpkv2GameLauncher(credentialUtils);

        lenient().when(credentialUtils.of(anyMap())).thenReturn(credentialAccessor);
        lenient().when(credentialAccessor.getValue(Credentials.CID)).thenReturn("cid-1");
        lenient().when(credentialAccessor.getValue(Credentials.COUNTRY)).thenReturn("EN");
        lenient().when(credentialAccessor.getValue(Credentials.API_TOKEN)).thenReturn("api-token");
        lenient().when(credentialAccessor.getValue(Credentials.PROVIDER_ID)).thenReturn("7");
        lenient().when(credentialAccessor.getValue(Credentials.SIGN_KEY)).thenReturn("sign-key");
    }

    private GameLaunchContext buildContext(String vendorLanguageCode) {
        return GameLaunchContext.builder()
                .vendorPlayerUsername("player123")
                .vendorGameCode("game-001")
                .vendorCurrencyCode("USD")
                .vendorLanguageCode(vendorLanguageCode)
                .vendorPlatformCode("desktop")
                .token("player-token")
                .lobbyUrl("https://operator.example.com/home")
                .ipAddress("1.1.1.1")
                .vendorCredentials(new HashMap<>())
                .build();
    }

    @Test
    void buildRequestBody_withVendorLanguageCode_sendsItAsLang() {
        GameLaunchRequest request = launcher.buildRequestBody(buildContext("es"));

        assertThat(request.getLang()).isEqualTo("es");
    }

    @Test
    void buildRequestBody_withVendorLanguageCode_serialisedBodyContainsLang() throws Exception {
        GameLaunchRequest request = launcher.buildRequestBody(buildContext("es"));

        String json = new ObjectMapper().writeValueAsString(request);

        assertThat(json).contains("\"lang\":\"es\"");
    }

    @Test
    void buildRequestBody_withVendorLanguageCode_otherFieldsStillPopulated() {
        GameLaunchRequest request = launcher.buildRequestBody(buildContext("es"));

        assertThat(request.getApi_token()).isEqualTo("api-token");
        assertThat(request.getCid()).isEqualTo("cid-1");
        assertThat(request.getPlayer_id()).isEqualTo("player123");
        assertThat(request.getProvider()).isEqualTo(7);
        assertThat(request.getGame()).isEqualTo("game-001");
        assertThat(request.getNickname()).isEqualTo("player123");
        assertThat(request.getCurrency()).isEqualTo("USD");
        assertThat(request.getPlayer_token()).isEqualTo("player-token");
        assertThat(request.getBalance()).isEqualTo("0");
        assertThat(request.getIp()).isEqualTo("1.1.1.1");
        assertThat(request.getCountry()).isEqualTo("EN");
        assertThat(request.getReturn_url()).isEqualTo("https://operator.example.com/home");
        assertThat(request.getClient_type()).isEqualTo("desktop");
    }

    @Test
    void getHeaders_signatureCoversLang() {
        GameLaunchContext spanish = buildContext("es");
        GameLaunchContext english = buildContext("en");

        Map<String, String> spanishHeaders = launcher.getHeaders(spanish, launcher.buildRequestBody(spanish));
        Map<String, String> englishHeaders = launcher.getHeaders(english, launcher.buildRequestBody(english));

        assertThat(spanishHeaders.get(SIGNATURE_HEADER)).isNotBlank();
        assertThat(spanishHeaders.get(SIGNATURE_HEADER)).isNotEqualTo(englishHeaders.get(SIGNATURE_HEADER));
    }

    @Test
    void buildRequestBody_withNullVendorLanguageCode_omitsLangFromSerialisedBody() throws Exception {
        GameLaunchRequest request = launcher.buildRequestBody(buildContext(null));

        assertThat(request.getLang()).isNull();
        assertThat(new ObjectMapper().writeValueAsString(request)).doesNotContain("\"lang\"");
    }

    @Test
    void buildRequestBody_withBlankVendorLanguageCode_omitsLangFromSerialisedBody() throws Exception {
        for (String blank : List.of("", "   ")) {
            GameLaunchRequest request = launcher.buildRequestBody(buildContext(blank));

            assertThat(request.getLang()).isNull();
            assertThat(new ObjectMapper().writeValueAsString(request)).doesNotContain("\"lang\"");
        }
    }

    @Test
    void getHeaders_withoutLang_signsTheBodyThatHasNoLangKey() throws Exception {
        GameLaunchContext context = buildContext(null);
        GameLaunchRequest request = launcher.buildRequestBody(context);

        String signedPayload = new ObjectMapper().writeValueAsString(request);
        String expectedSignature = SigningStrategyType.HMAC_SHA256_HEX.getStrategy().sign(signedPayload, "sign-key");

        assertThat(signedPayload).doesNotContain("lang");
        assertThat(launcher.getHeaders(context, request).get(SIGNATURE_HEADER)).isEqualTo(expectedSignature);
    }
}
