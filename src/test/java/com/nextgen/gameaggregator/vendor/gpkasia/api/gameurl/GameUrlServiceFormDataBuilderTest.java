package com.nextgen.gameaggregator.vendor.gpkasia.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Platforms;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.PlatformType;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@code GameUrlService.formDataBuilder(...)}, focused on the
 * OFRF-19 fix: the "home page url" form field name differs per underlying
 * GPK Asia provider - Bgaming (provider=9) expects snake_case
 * {@code "home_url"}, while Booming Games (provider=7) expects camelCase
 * {@code "homeUrl"}. Previously the field name was hardcoded to
 * {@code "home_url"} for every provider, breaking Booming Games' previous-URL
 * redirect behaviour.
 * <p>
 * <b>Unverified assumption:</b> {@code PlatformType.BOOMING} is assumed to
 * exist as the provider-id constant for Booming Games, following the same
 * naming convention as the {@code SEVENMOJO}/{@code SEVENMOJOLATAM}
 * constants already used in this class. If this constant doesn't exist
 * under this name, tell me the actual constant and I'll adjust.
 * <p>
 * {@code VendorService.trimGameCode(...)}, {@code VendorService.getCurrentTime()}
 * and {@code Platforms.checkPlatformCode(...)} are static utilities whose
 * internals I haven't seen - they're mocked via {@code mockStatic} so this
 * test only exercises {@code GameUrlService}'s own logic (the home-url
 * branching), not those collaborators' behaviour.
 */
class GameUrlServiceFormDataBuilderTest {

    private static final String LOBBY_URL = "https://lobby.example.com/return";
    private static final String VENDOR_GAME_CODE = "SLOT_001";
    private static final String VENDOR_PLAYER_USERNAME = "player123";
    private static final String LANGUAGE_CODE = "en";
    private static final String IP_ADDRESS = "123.123.123.123";
    private static final String TRIMMED_GAME_CODE = "SLOT_001"; // assumed passthrough, no "_stg"/"_STG" suffix
    private static final String CLIENT_TYPE = "desktop";
    private static final String API_TOKEN_VALUE = "some-api-token";
    private static final long FIXED_CURRENT_TIME = 1700000000L;

    private final GameUrlService gameUrlService = new GameUrlService();

    private GameSession buildGameSession() {
        GameSession gameSession = new GameSession();
        gameSession.setVendorGameCode(VENDOR_GAME_CODE);
        gameSession.setVendorPlayerUsername(VENDOR_PLAYER_USERNAME);
        gameSession.setLobbyUrl(LOBBY_URL);
        gameSession.setVendorLanguageCode(LANGUAGE_CODE);
        gameSession.setVendorPlatformCode("WEB");
        gameSession.setIpAddress(IP_ADDRESS);
        return gameSession;
    }

    private Map<String, String> buildCredentials(String platformId) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put(Credentials.api_token, API_TOKEN_VALUE);
        credentials.put(Credentials.platform_id, platformId);
        return credentials;
    }

    @Test
    void whenProviderIsBoomingGames_usesCamelCaseHomeUrlKey() throws InvalidVendorLineException, InvalidFormatException {
        GameSession gameSession = buildGameSession();
        Map<String, String> credentials = buildCredentials(PlatformType.BOOMING);

        try (MockedStatic<VendorService> vendorServiceMock = mockStatic(VendorService.class);
             MockedStatic<Platforms> platformsMock = mockStatic(Platforms.class)) {

            vendorServiceMock.when(() -> VendorService.trimGameCode(anyString())).thenReturn(TRIMMED_GAME_CODE);
            vendorServiceMock.when(VendorService::getCurrentTime).thenReturn(FIXED_CURRENT_TIME);
            platformsMock.when(() -> Platforms.checkPlatformCode(anyString())).thenReturn(CLIENT_TYPE);

            MultiValueMap<String, String> formData = gameUrlService.formDataBuilder(VENDOR_GAME_CODE, gameSession, credentials);

            assertThat(formData.getFirst("homeUrl")).isEqualTo(LOBBY_URL);
            assertThat(formData.containsKey("home_url"))
                    .as("home_url (snake_case) must NOT be present for Booming Games - only homeUrl (camelCase)")
                    .isFalse();
        }
    }

    @Test
    void whenProviderIsNotBoomingGames_usesSnakeCaseHomeUrlKey() throws InvalidVendorLineException, InvalidFormatException {
        GameSession gameSession = buildGameSession();
        // "9" = Bgaming per vendor API doc. Any provider other than
        // PlatformType.BOOMING should fall back to the original snake_case key.
        Map<String, String> credentials = buildCredentials("9");

        try (MockedStatic<VendorService> vendorServiceMock = mockStatic(VendorService.class);
             MockedStatic<Platforms> platformsMock = mockStatic(Platforms.class)) {

            vendorServiceMock.when(() -> VendorService.trimGameCode(anyString())).thenReturn(TRIMMED_GAME_CODE);
            vendorServiceMock.when(VendorService::getCurrentTime).thenReturn(FIXED_CURRENT_TIME);
            platformsMock.when(() -> Platforms.checkPlatformCode(anyString())).thenReturn(CLIENT_TYPE);

            MultiValueMap<String, String> formData = gameUrlService.formDataBuilder(VENDOR_GAME_CODE, gameSession, credentials);

            assertThat(formData.getFirst("home_url")).isEqualTo(LOBBY_URL);
            assertThat(formData.containsKey("homeUrl"))
                    .as("homeUrl (camelCase) must NOT be present for non-Booming providers - only home_url (snake_case)")
                    .isFalse();
        }
    }

    @Test
    void otherFormFieldsAreStillPopulatedCorrectly_regardlessOfHomeUrlBranch() throws InvalidVendorLineException, InvalidFormatException {
        GameSession gameSession = buildGameSession();
        Map<String, String> credentials = buildCredentials(PlatformType.BOOMING);

        try (MockedStatic<VendorService> vendorServiceMock = mockStatic(VendorService.class);
             MockedStatic<Platforms> platformsMock = mockStatic(Platforms.class)) {

            vendorServiceMock.when(() -> VendorService.trimGameCode(anyString())).thenReturn(TRIMMED_GAME_CODE);
            vendorServiceMock.when(VendorService::getCurrentTime).thenReturn(FIXED_CURRENT_TIME);
            platformsMock.when(() -> Platforms.checkPlatformCode(anyString())).thenReturn(CLIENT_TYPE);

            MultiValueMap<String, String> formData = gameUrlService.formDataBuilder(VENDOR_GAME_CODE, gameSession, credentials);

            assertThat(formData.getFirst("api_token")).isEqualTo(API_TOKEN_VALUE);
            assertThat(formData.getFirst("user")).isEqualTo(VENDOR_PLAYER_USERNAME);
            assertThat(formData.getFirst("password")).isEqualTo(VENDOR_PLAYER_USERNAME);
            assertThat(formData.getFirst("timestamp")).isEqualTo(String.valueOf(FIXED_CURRENT_TIME));
            assertThat(formData.getFirst("mode")).isEqualTo(TRIMMED_GAME_CODE);
            assertThat(formData.getFirst("lang")).isEqualTo(LANGUAGE_CODE);
            assertThat(formData.getFirst("ip")).isEqualTo(IP_ADDRESS);
            assertThat(formData.getFirst("client_type")).isEqualTo(CLIENT_TYPE);
        }
    }
}