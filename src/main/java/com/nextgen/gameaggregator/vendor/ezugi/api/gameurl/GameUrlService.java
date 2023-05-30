package com.nextgen.gameaggregator.vendor.ezugi.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.service.VendorService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("selectGame", gameSession.getVendorGameCode());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("token", gameSession.getToken());
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        GameUrlVo gameUrlVo = new GameUrlVo();
        // Get Game Lobby Url By Vendor Line
        String lobbyUrl = credentials.get(Credentials.LOBBY_URL);
        // Lookup Game Code (Baccarat,BlackJack,...)
        String selectGame = formData.get("selectGame").get(0);
        // Lookup Game language
        String language = formData.get("language").get(0);
        // Generate game session token to embed into urlScheme and save into game session table
        // Add LT to define launch token, because session and launch token cannot be same)
        String token = formData.get("token").get(0) + "LT";
        // Retrieve Operator Id as it is required to form the Game URL
        String operatorId = credentials.get(Credentials.OPERATOR_ID);
        // Construct the Game URL
        String gameUrl = VendorService.generateGameUrl(lobbyUrl, token, operatorId, language, selectGame);
        // Save this player's game session
        // Set the game URL and return to Operator
        gameUrlVo.setGameUrl(gameUrl);

        return gameUrlVo;
    }
}
