package com.nextgen.gameaggregator.vendor.ezugi.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.ezugi.constant.Credentials;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("openTable", gameSession.getVendorGameCode());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("token", gameSession.getToken());
        formData.add("operatorId", credentials.get(Credentials.OPERATOR_ID));
        formData.add("homeUrl", gameSession.getLobbyUrl());
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException {
        GameUrlVo gameUrlVo = new GameUrlVo();
        // Get Game Lobby Url By Vendor Line
        String lobbyUrl = credentials.get(Credentials.LOBBY_URL);

        // Construct the Game URL
        URI uri = UriComponentsBuilder.fromUriString(lobbyUrl)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        // Save this player's game session
        // Set the game URL and return to Operator
        gameUrlVo.setGameUrl(uri.toString());

        return gameUrlVo;
    }
}
