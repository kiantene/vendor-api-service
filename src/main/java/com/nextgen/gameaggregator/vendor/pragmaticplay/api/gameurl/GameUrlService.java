package com.nextgen.gameaggregator.vendor.pragmaticplay.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(Endpoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String secureLogin = Optional.ofNullable(credentials.get(Credentials.SECURE_LOGIN))
                .orElseThrow(InvalidVendorLineException::new);

        String secret = Optional.ofNullable(credentials.get(Credentials.SECRET_KEY))
                .orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secureLogin", secureLogin);
        formData.add("symbol", gameSession.getVendorGameCode());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("technology", "H5");
        formData.add("token", gameSession.getToken());
        formData.add("externalPlayerId", gameSession.getVendorPlayerUsername());
        formData.add("platform", gameSession.getVendorPlatformCode());
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("lobbyUrl", gameSession.getLobbyUrl());
        String hash = VendorService.generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }
}
