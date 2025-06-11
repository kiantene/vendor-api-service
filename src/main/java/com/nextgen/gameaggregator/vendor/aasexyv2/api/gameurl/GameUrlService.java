package com.nextgen.gameaggregator.vendor.aasexyv2.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.aasexyv2.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aasexyv2.constant.EndPoints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameUrlService extends BaseGameUrlService<GameUrlVo> {
    // credential value
    private String apiUrl;
    private String cert;
    private String agentId;
    private String betLimit;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setAutoMapResponse(false);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        this.apiUrl = ValidationUtils.validateCredential(credentials.get(Credentials.API_URL));
        this.cert = ValidationUtils.validateCredential(credentials.get(Credentials.CERT));
        this.agentId = ValidationUtils.validateCredential(credentials.get(Credentials.AGENT_ID));
        this.betLimit = ValidationUtils.validateCredential(credentials.get(Credentials.BET_LIMIT));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("cert", this.cert);
        formData.add("agentId", this.agentId);
        formData.add("userId", gameSession.getVendorPlayerUsername());
        formData.add("isMobileLogin", gameSession.getPlatformId().equals(1) ? "true" : "false");
        formData.add("externalURL", gameSession.getLobbyUrl());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("platform", "SEXYBCRT");
        formData.add("gameType", "LIVE");

        String vendorGameCode = gameSession.getVendorGameCode();
        if (vendorGameCode.contains("_")) {
            String[] gameCodeParts = vendorGameCode.split("_");
            formData.add("gameCode", gameCodeParts[0]);
            formData.add("hall", "SEXY");
            formData.add("isLaunchGameTable", "true");
            formData.add("gameTableId", gameCodeParts[1]);
        } else {
            formData.add("gameCode", vendorGameCode);
        }

        return formData;
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        try {
            this.checkAndCreateAccount(gameSession, httpRequestLog);
        } catch (Exception e) {
            throw new InvalidVendorResponseException("Failed to checkAndCreateAccount : " + e);
        }

        httpRequestLog.setUrl(this.apiUrl + EndPoints.GAME_URL);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        // Trigger doPost to get game url function by calling vendor api
        ResponseEntity<String> response = this.doPost(this.apiUrl, EndPoints.GAME_URL, new HttpHeaders(), formData, isTimeout);
        this.validateResponse(response, isTimeout, httpRequestLog, GameUrlVo.class, gameSession);

        return new Gson().fromJson(response.getBody(), GameUrlVo.class);
    }

    private void checkAndCreateAccount(GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("cert", this.cert);
        formData.add("agentId", this.agentId);
        formData.add("userId", gameSession.getVendorPlayerUsername());
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("betLimit", this.betLimit);
        formData.add("language", gameSession.getVendorLanguageCode());

        httpRequestLog.setUrl(this.apiUrl + EndPoints.CREATE_MEMBER);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        ResponseEntity<String> response = this.doPost(this.apiUrl, EndPoints.CREATE_MEMBER, new HttpHeaders(), formData, isTimeout);
        this.validateResponse(response, isTimeout, httpRequestLog, GameUrlVo.class, gameSession);

        GameUrlVo responseVo = new Gson().fromJson(response.getBody(), GameUrlVo.class);

        if (!responseVo.getStatus().equalsIgnoreCase("0000")
                && !responseVo.getStatus().equalsIgnoreCase("1001")) {
            throw new InvalidVendorResponseException("Failed to checkAndCreateAccount : " + response.getBody());
        }
    }
}
