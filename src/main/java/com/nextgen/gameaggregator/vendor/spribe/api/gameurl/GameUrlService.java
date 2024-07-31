package com.nextgen.gameaggregator.vendor.spribe.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.spribe.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spribe.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    // the following autowired is required due to reflection logic
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorService vendorService;

    private static final String OPERATOR = "operator";

    public GameUrlService() {
        super(GameUrlVo.class);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
                                                         Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        String operator = credentials.getOrDefault(OPERATOR, "");

        // Regenerate token (launch token only can be use once time)
        String newToken = UUID.randomUUID().toString();
        gameSession = gameSessionService.regenerateGameSessionToken(gameSession, newToken);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("token", newToken);
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add(OPERATOR, operator);

        return formData;
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorLineException {

        httpRequestLog.setUrl(SELF_GENERATED_GAME_URL);

        // Retrieve the game domain from the credentials map.
        String gameUrl = credentials.getOrDefault(Credentials.GAME_URL, "");
        if (gameUrl.isBlank()) {
            throw new InvalidVendorLineException();
        }

        String path = gameSession.getVendorGameCode();
        URI uri = URI.create("https://" + gameUrl + "/" + path + "?" + vendorService.toQueryString(formData));

        // Create a new GameUrlVo object and set the game URL as its value.
        GameUrlVo responseVo = new GameUrlVo();
        responseVo.setGameUrl(uri.toString());

        httpRequestLog.setOperatorHttpStatusCode(200);
        httpRequestLog.setBetEnd(System.currentTimeMillis());

        return responseVo;
    }
}
