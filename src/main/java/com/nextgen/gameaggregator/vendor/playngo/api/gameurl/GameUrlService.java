package com.nextgen.gameaggregator.vendor.playngo.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.playngo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playngo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.playngo.constant.GameType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("pid", credentials.get(Credentials.PRODUCT_GROUP));
        formData.add("gid", gameSession.getVendorGameCode());
        formData.add("channel", gameSession.getVendorPlatformCode());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("practice", GameType.REAL);
        formData.add("ticket", gameSession.getToken());
        formData.add("origin", gameSession.getLobbyUrl());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        GameUrlVo responseVo = new GameUrlVo();

        //Get Vendor game URL
        String urlScheme = credentials.get(Credentials.API_URL);

        //Construct the Game URL
        // Construct the Game URL
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.GAME_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        //Save this player's game session
        //Set the game URL and return to Operator
        responseVo.setGameUrl(uri.toString());

        return responseVo;
    }

}
