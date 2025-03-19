package com.nextgen.gameaggregator.vendor.ygg.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ygg.constant.Credentials;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@Getter
public class GameUrlService extends BaseGameUrlService<GameUrlVo> {

    private String launchUrl;
    private String orgs;
    private String license;

    public GameUrlService() {
        super(GameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
                                                         Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        this.launchUrl = ValidationUtils.validateCredential(credentials.get(Credentials.LAUNCH_URL));
        this.orgs = ValidationUtils.validateCredential(credentials.get(Credentials.ORG));
        this.license = ValidationUtils.validateCredential(credentials.get(Credentials.LICENSE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("gameid", gameCode); //cid from vendor
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("currency", gameSession.getVendorCurrencyCode()); //game code
        formData.add("org", this.getOrgs());
        formData.add("channel", gameSession.getVendorPlatformCode());
        formData.add("home", gameSession.getLobbyUrl());
        formData.add("key", gameSession.getToken());
        formData.add("license", this.license);

        return formData;
    }


    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                  GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        URI uri = UriComponentsBuilder.fromUriString(this.getLaunchUrl())
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> response = ResponseEntity.ok().body(uri.toString());

        this.validateResponse(response, isTimeout, httpRequestLog, GameUrlVo.class, gameSession);

        GameUrlVo responseVo = new GameUrlVo();
        responseVo.setGameUrl(uri.toString());
        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }

}
