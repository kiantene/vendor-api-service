package com.nextgen.gameaggregator.vendor.mg.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.vendor.mg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mg.service.VendorTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameUrlService extends BaseGameUrlService<GameUrlVo> {
    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorTokenService vendorTokenService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
                                                         Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        try {
            formData.add("contentCode", gameSession.getVendorGameCode());
            formData.add("platform", gameSession.getVendorPlatformCode());
            formData.add("langCode", gameSession.getVendorLanguageCode());

        } catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }
        return formData;
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {

        String apiUrl = credentials.get(Credentials.API_URL);
        String uri = "/agents/" + credentials.get(Credentials.AGENT_CODE)
                + "/players/" + gameSession.getVendorPlayerUsername()
                + "/sessions";
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        String token = vendorTokenService.getToken(gameSession.getVendorLineId());

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        headerMap.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        HttpHeaders httpHeaders = new HttpHeaders(headerMap);

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        GameUrlVo responseVo = null;

        ResponseEntity<String> response = this.doPost(apiUrl, uri, httpHeaders, formData, isTimeout);

        this.validateResponse(response, isTimeout, httpRequestLog, GameUrlVo.class, gameSession);

        responseVo = new Gson().fromJson(response.getBody(), GameUrlVo.class);

        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }
}
