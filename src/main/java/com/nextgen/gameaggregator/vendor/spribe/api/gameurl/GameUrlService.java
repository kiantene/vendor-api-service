package com.nextgen.gameaggregator.vendor.spribe.api.gameurl;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.spribe.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spribe.service.VendorService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorService vendorService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
            Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        // Get operator and token by vendor line
        String operator = "";
        try {
            operator = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator");
        } catch (CredentialNotFoundException e) {
            log.error("Credential not found : " + e.getMessage());
        }

        // Regenerate token (launch token only can be use once time)
        String newToken = UUID.randomUUID().toString();
        gameSession = gameSessionService.regenerateGameSessionToken(gameSession, newToken);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("token", newToken);
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("operator", operator);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
            GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
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
        return responseVo;
    }
}
