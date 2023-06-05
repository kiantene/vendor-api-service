package com.nextgen.gameaggregator.vendor.booongo.api.gameurl;

import java.util.Map;

import com.nextgen.gameaggregator.vendor.booongo.constant.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.nextgen.gameaggregator.vendor.booongo.service.VendorService;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("platform", gameSession.getVendorPlatformCode());
        formData.add("gameCode", gameCode);
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("token", gameSession.getToken());
//        formData.add("wl", credentials.get(Credentials.WL));
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException{

        GameUrlVo gameUrlVo = new GameUrlVo();

        String API_URL = credentials.get(Credentials.API_URL);
        String PROJECT_NAME = credentials.get(Credentials.PROJECT_NAME);
        String token = formData.get("token").get(0);
        String platform = formData.get("platform").get(0);
        String gameCode = formData.get("gameCode").get(0);
        String lang = formData.get("lang").get(0);

        //combine all string and generate game url
        gameUrlVo.setGameUrl(VendorService.generateGameUrl(API_URL, PROJECT_NAME, token, platform, gameCode, lang));

        return gameUrlVo;
    }
}
