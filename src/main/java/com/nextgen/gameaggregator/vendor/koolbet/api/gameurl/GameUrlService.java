package com.nextgen.gameaggregator.vendor.koolbet.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Languages;
import com.nextgen.gameaggregator.vendor.koolbet.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<KBGameUrlVo> {

    private String launchUrl;
    private String agentId;
    private String apiToken;

    public GameUrlService() {
        super(KBGameUrlVo.class);
        this.setAutoMapResponse(false);
        this.setHttpMethod(HttpMethod.GET);
        this.setContentType(MediaType.APPLICATION_JSON);
        this.setCredentialApiUrl(Credentials.API_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        this.launchUrl = ValidationUtils.validateCredential(credentials.get(Credentials.API_URL));
        this.agentId = ValidationUtils.validateCredential(credentials.get(Credentials.AGENT_ID));
        this.apiToken = ValidationUtils.validateCredential(credentials.get(Credentials.API_TOKEN));

        //Construct Param
        Map<String, String> params = new LinkedHashMap<>();
        params.put("Token", gameSession.getToken());
        params.put("GameId", gameSession.getVendorGameCode());
        params.put("Lang", gameSession.getVendorLanguageCode());

        //Encrypt param before sending
        String key = VendorService.generateKey(params, this.agentId, this.apiToken);

        //setup form data
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Token", gameSession.getToken());
        formData.add("GameId", gameSession.getVendorGameCode());
        formData.add("Lang", Languages.getLanguageCode(gameSession.getVendorLanguageCode()));
        formData.add("HomeUrl", gameSession.getLobbyUrl());
        formData.add("Platform", gameSession.getVendorPlatformCode());
        formData.add("AgentId", this.agentId);
        formData.add("Key", key);

        return formData;
    }

    @Override
    public KBGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                    GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        ResponseEntity<String> response = this.doGet(this.launchUrl, EndPoints.GAME_URL, formData, isTimeout);

        this.validateResponse(response, isTimeout, httpRequestLog, KBGameUrlVo.class, gameSession);

        KBGameUrlVo responseVo = new Gson().fromJson(response.getBody(), KBGameUrlVo.class);

        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }
}
