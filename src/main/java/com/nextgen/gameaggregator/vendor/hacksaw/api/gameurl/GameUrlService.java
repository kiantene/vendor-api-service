package com.nextgen.gameaggregator.vendor.hacksaw.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksaw.service.VendorService;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.Optional;

@Service
public class GameUrlService implements GameUrl {

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String casinoId = credentials.get(Credentials.CASINO_ID);
        Optional.ofNullable(casinoId).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("casinoid", casinoId);
        formData.add("gameid", gameSession.getVendorGameCode());
        formData.add("launchtoken", gameSession.getToken());
        formData.add("playmode", "real");
        formData.set("currencycode", gameSession.getVendorCurrencyCode());
        formData.add("clienttype", gameSession.getVendorPlatformCode());
        formData.add("languagecode", gameSession.getVendorLanguageCode());
        formData.add("exiturl", gameSession.getLobbyUrl());
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        GameUrlVo responseVo = new GameUrlVo();

        //Get Vendor game URL
        String urlScheme = credentials.get(Credentials.WEB_URL);
        //Construct the Game URL
        String gameUrl = VendorService.generateGameUrl(urlScheme, formData);

        //Save this player's game session
        //Set the game URL and return to Operator
        responseVo.setGameUrl(gameUrl);

        return responseVo;
    }

}
