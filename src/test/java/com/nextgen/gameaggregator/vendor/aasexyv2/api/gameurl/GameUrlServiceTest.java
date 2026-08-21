package com.nextgen.gameaggregator.vendor.aasexyv2.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.vendor.aasexyv2.constant.Credentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameUrlServiceTest {

    private final GameUrlService gameUrlService = new GameUrlService();

    private Map<String, String> credentials() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put(Credentials.API_URL, "https://api.aasexy.com");
        credentials.put(Credentials.CERT, "cert-123");
        credentials.put(Credentials.AGENT_ID, "agent-123");
        credentials.put(Credentials.BET_LIMIT, "{\"SEXYBCRT\":{\"LIVE\":{\"limitId\":[280301,280303]}}}");
        return credentials;
    }

    private GameSession gameSession(String vendorGameCode) {
        GameSession gameSession = new GameSession();
        gameSession.setVendorPlayerUsername("player1");
        gameSession.setPlatformId(2);
        gameSession.setLobbyUrl("https://lobby.example.com");
        gameSession.setVendorLanguageCode("en");
        gameSession.setVendorGameCode(vendorGameCode);
        return gameSession;
    }

    @Nested
    @DisplayName("formDataBuilder Tests")
    class FormDataBuilderTests {

        @Test
        @DisplayName("Should include betLimit for a standard (non-table) game launch")
        void formDataBuilder_StandardGame_IncludesBetLimit() throws InvalidVendorLineException, InvalidFormatException {
            MultiValueMap<String, String> formData = gameUrlService.formDataBuilder("MX-LIVE-001", gameSession("MX-LIVE-001"), credentials());

            assertNotNull(formData);
            assertEquals("{\"SEXYBCRT\":{\"LIVE\":{\"limitId\":[280301,280303]}}}", formData.getFirst("betLimit"));
            assertEquals("MX-LIVE-001", formData.getFirst("gameCode"));
        }

        @Test
        @DisplayName("Should include betLimit alongside table-launch params when gameCode has a table suffix")
        void formDataBuilder_TableGame_IncludesBetLimitAndTableParams() throws InvalidVendorLineException, InvalidFormatException {
            MultiValueMap<String, String> formData = gameUrlService.formDataBuilder("MX-LIVE-001_1", gameSession("MX-LIVE-001_1"), credentials());

            assertNotNull(formData);
            assertEquals("{\"SEXYBCRT\":{\"LIVE\":{\"limitId\":[280301,280303]}}}", formData.getFirst("betLimit"));
            assertEquals("MX-LIVE-001", formData.getFirst("gameCode"));
            assertEquals("SEXY", formData.getFirst("hall"));
            assertEquals("true", formData.getFirst("isLaunchGameTable"));
            assertEquals("1", formData.getFirst("gameTableId"));
        }

        @Test
        @DisplayName("Should throw when betLimit credential is missing")
        void formDataBuilder_MissingBetLimit_Throws() {
            Map<String, String> credentials = credentials();
            credentials.remove(Credentials.BET_LIMIT);

            assertThrows(InvalidVendorLineException.class,
                    () -> gameUrlService.formDataBuilder("MX-LIVE-001", gameSession("MX-LIVE-001"), credentials));
        }
    }
}
