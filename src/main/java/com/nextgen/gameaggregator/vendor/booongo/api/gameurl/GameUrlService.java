package com.nextgen.gameaggregator.vendor.booongo.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.GeoIpUtil;
import com.nextgen.gameaggregator.vendor.booongo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.booongo.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;
    private static final String LOBBY = "lobby";

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String country = GeoIpUtil.getCountryCode(gameSession.getIpAddress());
        formData.add("platform", gameSession.getVendorPlatformCode());
        if (!isLobbyGame(gameCode)) {
            formData.add("game", gameCode);
        }
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("token", gameSession.getToken());
        formData.add("country", country);
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {

        GameUrlVo gameUrlVo = new GameUrlVo();

        String apiUrl = credentials.get(Credentials.API_URL);
        String projectName = credentials.get(Credentials.PROJECT_NAME);
        String vendorPagePath = resolveVendorPagePath(projectName, gameSession);

        formData.add("WL", credentials.get(Credentials.WL));

        //combine all string and generate game url
        gameUrlVo.setGameUrl(generateGameUrl(apiUrl, vendorPagePath, formData));

        return gameUrlVo;
    }

    public static String generateGameUrl(
            String apiUrl,
            String vendorPagePath,
            MultiValueMap<String, String> formData
    ) {
        return UriComponentsBuilder
                .fromHttpUrl(apiUrl)
                .path(vendorPagePath)
                .queryParams(formData)   // converts formData → query string
                .build()
                .encode()                // URL-encode safely
                .toUriString();
    }

    private String resolveVendorPagePath(String projectName, GameSession gameSession) {
        return projectName + (isLobbyGame(gameSession.getVendorGameCode()) ? EndPoints.LOBBY_PAGE : EndPoints.GAME_PAGE);
    }

    private boolean isLobbyGame(String vendorGameCode) {
        return LOBBY.equalsIgnoreCase(vendorGameCode);
    }
}
