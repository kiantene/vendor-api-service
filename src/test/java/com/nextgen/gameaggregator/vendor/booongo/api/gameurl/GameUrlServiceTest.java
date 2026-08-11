package com.nextgen.gameaggregator.vendor.booongo.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.GeoIpUtil;
import com.nextgen.gameaggregator.vendor.booongo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.booongo.constant.EndPoints;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class GameUrlServiceTest {

    @Mock
    private RequestService requestService;

    @InjectMocks
    private GameUrlService gameUrlService;

    private MockedStatic<GeoIpUtil> geoIpUtilMockedStatic;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gameUrlService, "profilesActive", "test");
        geoIpUtilMockedStatic = mockStatic(GeoIpUtil.class);
    }

    @AfterEach
    void tearDown() {
        geoIpUtilMockedStatic.close();
    }

    @Nested
    @DisplayName("formDataBuilder Tests")
    class FormDataBuilderTests {

        @Test
        @DisplayName("Should build form data for standard game code (non-lobby)")
        void formDataBuilder_StandardGame_Success() throws InvalidVendorLineException, InvalidFormatException {
            // Arrange
            GameSession gameSession = new GameSession();
            gameSession.setIpAddress("192.168.1.1");
            gameSession.setVendorPlatformCode("desktop");
            gameSession.setVendorLanguageCode("en");
            gameSession.setToken("session-token-123");

            geoIpUtilMockedStatic.when(() -> GeoIpUtil.getCountryCode("192.168.1.1"))
                    .thenReturn("US");

            // Act
            MultiValueMap<String, String> formData = gameUrlService.formDataBuilder("game_123", gameSession, new HashMap<>());

            // Assert
            assertNotNull(formData);
            assertEquals("desktop", formData.getFirst("platform"));
            assertEquals("game_123", formData.getFirst("game"));
            assertEquals("en", formData.getFirst("lang"));
            assertEquals("session-token-123", formData.getFirst("token"));
            assertEquals("US", formData.getFirst("country"));
        }

        @Test
        @DisplayName("Should build form data without 'game' param when gameCode is 'lobby'")
        void formDataBuilder_LobbyGame_ExcludesGameParam() throws InvalidVendorLineException, InvalidFormatException {
            // Arrange
            GameSession gameSession = new GameSession();
            gameSession.setIpAddress("10.0.0.1");
            gameSession.setVendorPlatformCode("mobile");
            gameSession.setVendorLanguageCode("th");
            gameSession.setToken("token-456");

            geoIpUtilMockedStatic.when(() -> GeoIpUtil.getCountryCode("10.0.0.1"))
                    .thenReturn("TH");

            // Act
            MultiValueMap<String, String> formData = gameUrlService.formDataBuilder("LOBBY", gameSession, new HashMap<>());

            // Assert
            assertNotNull(formData);
            assertEquals("mobile", formData.getFirst("platform"));
            assertNull(formData.getFirst("game")); // lobby game must not set game code
            assertEquals("th", formData.getFirst("lang"));
            assertEquals("token-456", formData.getFirst("token"));
            assertEquals("TH", formData.getFirst("country"));
        }
    }

    @Nested
    @DisplayName("call Tests")
    class CallTests {

        @Test
        @DisplayName("Should generate valid game URL for a standard game")
        void call_StandardGame_ReturnsGameUrlVo() throws InvalidVendorLineException {
            // Arrange
            GameSession gameSession = new GameSession();
            gameSession.setVendorGameCode("slot_game_code");

            Map<String, String> credentials = new HashMap<>();
            credentials.put(Credentials.API_URL, "https://api.booongo.com");
            credentials.put(Credentials.PROJECT_NAME, "myProject");
            credentials.put(Credentials.WL, "whiteLabel123");

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("platform", "desktop");
            formData.add("lang", "en");

            // Act
            GameUrlVo response = gameUrlService.call(formData, credentials, gameSession);

            // Assert
            assertNotNull(response);
            assertEquals("whiteLabel123", formData.getFirst("WL"));

            String expectedUrl = "https://api.booongo.com/myProject" + EndPoints.GAME_PAGE + "?platform=desktop&lang=en&WL=whiteLabel123";
            assertEquals(expectedUrl, response.getGameUrl());
        }

        @Test
        @DisplayName("Should generate valid game URL with lobby endpoint when vendorGameCode is 'lobby'")
        void call_LobbyGame_UsesLobbyEndpoint() throws InvalidVendorLineException {
            // Arrange
            GameSession gameSession = new GameSession();
            gameSession.setVendorGameCode("lobby");

            Map<String, String> credentials = new HashMap<>();
            credentials.put(Credentials.API_URL, "https://api.booongo.com");
            credentials.put(Credentials.PROJECT_NAME, "myProject");
            credentials.put(Credentials.WL, "wl_code");

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

            // Act
            GameUrlVo response = gameUrlService.call(formData, credentials, gameSession);

            // Assert
            assertNotNull(response);
            String expectedUrl = "https://api.booongo.com/myProject" + EndPoints.LOBBY_PAGE + "?WL=wl_code";
            assertEquals(expectedUrl, response.getGameUrl());
        }
    }

    @Nested
    @DisplayName("generateGameUrl Static Method Tests")
    class GenerateGameUrlTests {

        @Test
        @DisplayName("Should construct properly formatted and encoded URL")
        void generateGameUrl_ValidInputs_ReturnsEncodedUrl() {
            // Arrange
            String apiUrl = "https://game.booongo.com";
            String vendorPagePath = "/project/launch";
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("token", "abc 123");
            formData.add("lang", "en");

            // Act
            String url = GameUrlService.generateGameUrl(apiUrl, vendorPagePath, formData);

            // Assert
            // "abc 123" should be encoded to "abc%20123"
            assertEquals("https://game.booongo.com/project/launch?token=abc%20123&lang=en", url);
        }
    }

    @Test
    @DisplayName("Should omit 'country' param when GeoIpUtil returns null")
    void formDataBuilder_NullCountry_OmitsCountryParam() throws InvalidVendorLineException, InvalidFormatException {
        // Arrange
        GameSession gameSession = new GameSession();
        gameSession.setIpAddress("192.168.1.1");
        gameSession.setVendorPlatformCode("desktop");
        gameSession.setVendorLanguageCode("en");
        gameSession.setToken("session-token-123");

        geoIpUtilMockedStatic.when(() -> GeoIpUtil.getCountryCode("192.168.1.1"))
                .thenReturn(null);

        // Act
        MultiValueMap<String, String> formData = gameUrlService.formDataBuilder("game_123", gameSession, new HashMap<>());

        // Assert
        assertNotNull(formData);
        assertNull(formData.getFirst("country"));
        assertFalse(formData.containsKey("country"));
    }
}