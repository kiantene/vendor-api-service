package com.nextgen.gameaggregator.vendor.spadegaming.api.gameurl;

import java.net.URI;
import java.util.Map;

import com.nextgen.gameaggregator.entity.RawGameSession;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;

public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, RawGameSession rawGameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("acctId", rawGameSession.getVendorPlayerUsername());
        formData.add("token", rawGameSession.getToken());
        formData.add("game", gameCode);
        formData.add("language", rawGameSession.getVendorLanguageCode());
        if (rawGameSession.getPlatformId() != 2) formData.add("mobile", "true");
    
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, RawGameSession rawGameSession)
    throws InvalidVendorLineException, InvalidVendorResponseException {
        // Retrieve the game domain from the credentials map.
        String gameDomain = credentials.getOrDefault(Credentials.GAME_DOMAIN, "");
        if (gameDomain.isBlank()) {
            throw new InvalidVendorLineException();
        }

        // Build the URI needed to call the Spadegaming game URL API.
        URI uri = UriComponentsBuilder.newInstance()
            .scheme("https")
            .host(gameDomain)
            .path(credentials.getOrDefault(Credentials.MERCHANT_CODE, "") + "/")
            .path(credentials.getOrDefault(Credentials.API_INTERFACE, "") + "/")
            .queryParams(formData)
            .build()
            .encode()
            .toUri();

        // Create a new GameUrlVo object and set the game URL as its value.
        GameUrlVo responseVo = new GameUrlVo();
        responseVo.setGameUrl(uri.toString());
        return responseVo;
    }
}
