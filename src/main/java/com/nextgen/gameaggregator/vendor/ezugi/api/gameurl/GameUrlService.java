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
        formData.add("selectGame", gameCode);
        formData.add("language", gameSession.getLanguage());
        formData.add("token", gameSession.getToken());
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        GameUrlVo gameUrlVo = new GameUrlVo();

        // 1. Get Game Lobby Url By Vendor Line
        String lobbyUrl = credentials.get(Credentials.LOBBY_URL);
        // 2. Lookup Game Code (Baccarat,BlackJack,...)
        String selectGame = formData.get("selectGame").get(0);
        // 3. Lookup Game language
        String language = formData.get("language").get(0);
        // 4. Generate game session token to embed into urlScheme and save into game session table
        String token = formData.get("token").get(0);
        // 5. Retrieve Operator Id as it is required to form the Game URL
        String operatorId = credentials.get(Credentials.OPERATOR_ID);
        // 9. Construct the Game URL
        String gameUrl = VendorService.generateGameUrl(lobbyUrl, token, operatorId, language, selectGame);
        // 10. Save this player's game session
        // Set the game URL and return to Operator
        gameUrlVo.setGameUrl(gameUrl);

        return gameUrlVo;
    }
}
