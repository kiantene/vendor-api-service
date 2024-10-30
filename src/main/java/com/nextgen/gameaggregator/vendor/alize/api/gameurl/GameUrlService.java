package com.nextgen.gameaggregator.vendor.alize.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.alize.constant.Credentials;
import com.nextgen.gameaggregator.vendor.alize.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alize.constant.GameId;
import com.nextgen.gameaggregator.vendor.alize.service.VendorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@Slf4j
@Getter
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private GameSessionService gameSessionService;

    private String apiKey;
    private String apiSecret;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(Endpoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        this.apiKey = ValidationUtils.validateCredential(credentials.get(Credentials.API_KEY));
        this.apiSecret = ValidationUtils.validateCredential(credentials.get(Credentials.SECRET_KEY));

        // Get operator and gameUrl by vendor line
        String operator;
        String gameUrl;
        try {
            operator = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator");
            gameUrl = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "gameUrl");
        } catch (Exception e) {
            throw new InvalidVendorLineException(e.toString());
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("gameId", GameId.getGameId(gameSession.getVendorGameCode()));
        formData.add("gamecode", gameSession.getVendorGameCode());
        formData.add("ip", gameSession.getIpAddress());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("operator", operator);
        formData.add("player", gameSession.getVendorPlayerUsername());
        formData.add("playmode", "free");
        formData.add("returnURL", gameSession.getLobbyUrl());
        formData.add("timestamp", String.valueOf(System.currentTimeMillis()));
        formData.add("url", gameUrl);

        return formData;
    }

    @Override
    protected HttpHeaders getHeaders(HttpHeaders httpHeaders, MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) {

        String signature = VendorService.generateHash(this.getApiSecret(), new Gson().toJson(formData.toSingleValueMap()));

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add("X-API-Key", this.getApiKey());
        headerMap.add("X-Signature", signature);

        return new HttpHeaders(headerMap);
    }

    @Override
    protected GameUrlVo onResponseSuccess(GameUrlVo responseVo, GameSession gameSession) {

        String newToken = responseVo.getData().getToken();
        gameSessionService.regenerateGameSessionToken(gameSession, newToken);
        return responseVo;
    }
}
