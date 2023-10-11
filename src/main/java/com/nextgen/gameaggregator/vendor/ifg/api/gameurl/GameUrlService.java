package com.nextgen.gameaggregator.vendor.ifg.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.ifg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ifg.constant.GameType;
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

        formData.add("partner", credentials.get(Credentials.partner));
        formData.add("gameName", gameSession.getVendorGameCode());
        formData.add("platform", gameSession.getVendorPlatformCode());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("demo", GameType.demo_false);
        formData.add("key", gameSession.getToken());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        GameUrlVo responseVo = new GameUrlVo();

        //Get Vendor game URL
        String urlScheme = credentials.get(Credentials.game_url);

        //Construct the Game URL
        // Construct the Game URL
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        //Set the game URL and return to Operator
        responseVo.setGameUrl(uri.toString());

        return responseVo;
    }

}
