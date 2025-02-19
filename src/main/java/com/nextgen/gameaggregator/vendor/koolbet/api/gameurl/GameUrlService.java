package com.nextgen.gameaggregator.vendor.koolbet.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.koolbet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.koolbet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.koolbet.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<KBGameUrlVo> {

    private String agentId;
    private String apiToken;

    public GameUrlService() {
        super(KBGameUrlVo.class);
        this.setHttpMethod(HttpMethod.GET);
        this.setContentType(MediaType.APPLICATION_JSON);
        this.setCredentialApiUrl(Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

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
        formData.add("Lang", gameSession.getVendorLanguageCode());
        formData.add("HomeUrl", gameSession.getLobbyUrl());
        formData.add("Platform", gameSession.getVendorPlatformCode());
        formData.add("AgentId", this.agentId);
        formData.add("Key", key);

        return formData;
    }
}
