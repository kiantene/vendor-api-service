package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    private String key;
    private String projId;
    private String apiUrl;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setHttpMethod(HttpMethod.GET);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        this.key = ValidationUtils.validateCredential(credentials.get(Credentials.KEY));
        this.projId = ValidationUtils.validateCredential(credentials.get(Credentials.PROJ_ID));
        this.apiUrl = ValidationUtils.validateCredential(credentials.get(Credentials.API_URL));


        SettingsDto settings = new SettingsDto();
        settings.setUser_id(gameSession.getVendorPlayerUsername());
        settings.setExit_url(gameSession.getLobbyUrl());
        settings.setLanguage(gameSession.getVendorLanguageCode());
        settings.setHttps(Formats.SETTINGS_HTTPS);

        GameUrlDto gameUrlDto = new GameUrlDto();
        gameUrlDto.setProject(projId);
        gameUrlDto.setVersion(Formats.VERSION);
        gameUrlDto.setToken(gameSession.getToken());
        gameUrlDto.setGame(gameSession.getVendorGameCode());
        gameUrlDto.setSettings(settings);
        gameUrlDto.setDenomination(Formats.DENOMINATION);
        gameUrlDto.setCurrency(gameSession.getVendorCurrencyCode());
        gameUrlDto.setReturn_url_info(Formats.RETURN_URL_INFO);
        gameUrlDto.setCallback_version(Formats.CALLBACK_VERSION);

        Map<String, Object> mapData = VendorService.convertObjectToMap(gameUrlDto, LinkedHashMap.class);
        VendorService.rearrangeMap(mapData);
        MultiValueMap<String, String> formData = VendorService.flattenMapIntoMultiValueMap(mapData, "");
        formData.add("signature", VendorService.md5(VendorService.buildSignature(formData, key)));

        return formData;
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        GameUrlVo responseVo = null;
        ResponseEntity<String> response = this.doGet(apiUrl, EndPoints.GAME_URL, formData, isTimeout);

        this.validateResponse(response, isTimeout, httpRequestLog, GameUrlVo.class, gameSession);

        responseVo = new Gson().fromJson(response.getBody(), GameUrlVo.class);

        return responseVo;
    }
}
