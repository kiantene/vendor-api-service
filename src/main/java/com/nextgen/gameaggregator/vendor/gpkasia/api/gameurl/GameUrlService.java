package com.nextgen.gameaggregator.vendor.gpkasia.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Platforms;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    VendorService vendorService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("api_token", credentials.get(Credentials.api_token));
        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("password", gameSession.getVendorPlayerUsername());
        formData.add("platform", credentials.get(Credentials.platform_id));
        formData.add("timestamp", String.valueOf(vendorService.getCurrentTime()));
        formData.add("mode", gameSession.getVendorGameCode());
        formData.add("home_url", gameSession.getLobbyUrl());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("client_type", Platforms.checkPlatformCode(gameSession.getVendorPlatformCode()));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException{
        GameUrlVo gameUrlVo = new GameUrlVo();

        return gameUrlVo;
    }
}
