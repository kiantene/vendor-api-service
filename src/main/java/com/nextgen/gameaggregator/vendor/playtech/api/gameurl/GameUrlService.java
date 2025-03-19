package com.nextgen.gameaggregator.vendor.playtech.api.gameurl;


import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playtech.constant.Credentials;
import com.nextgen.gameaggregator.vendor.playtech.constant.EndPoints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GameUrlService extends BaseGameUrlService<PlayTechGameUrlVo> {

    public GameUrlService() {
        super(PlayTechGameUrlVo.class);
        this.setAutoMapResponse(false);
        this.setGameUrl(EndPoints.LAUNCH_GAME);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
                                                         Map<String, String> credentials)
            throws InvalidVendorLineException,
            InvalidFormatException {

        String kioskPrefix = ValidationUtils.validateCredential(credentials.get(Credentials.KIOSK_PREFIX));
        String serveName = ValidationUtils.validateCredential(credentials.get(Credentials.SERVE_NAME));

        return createFormData(gameCode, gameSession, serveName, kioskPrefix);
    }

    private MultiValueMap<String, String> createFormData(String gameCode, GameSession gameSession, String serveName, String kioskPrefix) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("requestId", gameSession.getTraceId());
        formData.add("serverName", serveName);
        formData.add("username", kioskPrefix + "_" + gameSession.getVendorPlayerUsername());
        formData.add("gameCodeName", gameCode);
        formData.add("clientPlatform", gameSession.getVendorPlatformCode());
        formData.add("externalToken", kioskPrefix + "_" + gameSession.getToken());
        formData.add("language", gameSession.getVendorLanguageCode());
        return formData;
    }

    @Override
    public PlayTechGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                          GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException,
            TimeoutException,
            InvalidVendorLineException {

        HttpHeaders httpHeaders = new HttpHeaders();
        String launchGameUrl = ValidationUtils.validateCredential(credentials.get(Credentials.API_URL));
        HttpHeaders headers = this.getHeaders(httpHeaders, formData, credentials, gameSession);
        httpRequestLog.setUrl(launchGameUrl + EndPoints.LAUNCH_GAME);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        ResponseEntity<String> response = this.doPost(launchGameUrl, EndPoints.LAUNCH_GAME, headers, formData,
                isTimeout);
        this.validateResponse(response, isTimeout, httpRequestLog, PlayTechGameUrlVo.class, gameSession);

        return new Gson().fromJson(response.getBody(), PlayTechGameUrlVo.class);
    }

    @Override
    protected HttpHeaders getHeaders(HttpHeaders httpHeaders, MultiValueMap<String, String> formData,
                                     Map<String, String> credentials, GameSession gameSession) {

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        String apiKey = credentials.getOrDefault(Credentials.API_KEY, "");
        headerMap.add("x-auth-kiosk-key", apiKey);
        return new HttpHeaders(headerMap);
    }
}
