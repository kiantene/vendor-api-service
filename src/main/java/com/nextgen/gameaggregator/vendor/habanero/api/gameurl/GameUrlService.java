package com.nextgen.gameaggregator.vendor.habanero.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("brandid", credentials.get(Credentials.BRAND_ID));
        formData.add("token", gameSession.getToken());
        formData.add("keyname", gameSession.getVendorGameCode());
        formData.add("mode", "real");
        formData.add("locale", gameSession.getVendorLanguageCode());
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        GameUrlVo responseVo = new GameUrlVo();

        //Construct the Game URL
        String gameUrl = UriComponentsBuilder.fromUriString(credentials.get(Credentials.API_URL))
                .path(EndPoints.GAME_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri()
                .toString();

        //Save this player's game session
        //Set the game URL and return to Operator
        responseVo.setGameUrl(gameUrl);

        return responseVo;
    }

}
