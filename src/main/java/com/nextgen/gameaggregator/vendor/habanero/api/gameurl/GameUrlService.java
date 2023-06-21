package com.nextgen.gameaggregator.vendor.habanero.api.gameurl;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.habanero.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.habanero.constant.Credentials;
import com.nextgen.gameaggregator.vendor.habanero.service.VendorService;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

        //Get Vendor game URL
        String urlScheme = credentials.get(Credentials.API_URL) + EndPoints.GAME_URL;
        //Construct the Game URL
        String gameUrl = VendorService.generateUrl(urlScheme, formData);

        //Save this player's game session
        //Set the game URL and return to Operator
        responseVo.setGameUrl(gameUrl);

        return responseVo;
    }

}
