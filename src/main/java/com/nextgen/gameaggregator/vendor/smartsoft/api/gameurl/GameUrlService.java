package com.nextgen.gameaggregator.vendor.smartsoft.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.EndPoints;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


public class GameUrlService extends BaseGameUrlService<SSGameUrlVo> {

    private String portalName;

    public GameUrlService() {
        super(SSGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        this.portalName = ValidationUtils.validateCredential(credentials.get(Credentials.PORTAL_NAME));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("GameName", gameSession.getVendorGameCode());
        formData.add("Token", gameSession.getToken());
        formData.add("ReturnUrl", gameSession.getLobbyUrl());
        formData.add("Lang", gameSession.getLanguage());
        formData.add("PortalName", portalName);

        return formData;
    }

    @Override
    public SSGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                    GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, InvalidVendorLineException, TimeoutException {
        //construct API address
        String launchUrl = Optional.of(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        URI url = UriComponentsBuilder.fromUriString(launchUrl)
                .path(EndPoints.LAUNCH_GAME)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        // Trigger doPost to get game url function by calling vendor api
        ResponseEntity<String> response = this.doGet(launchUrl, EndPoints.LAUNCH_GAME, formData, isTimeout);

        this.validateResponse(response, isTimeout, httpRequestLog, SSGameUrlVo.class, gameSession);

        SSGameUrlVo responseVo = new SSGameUrlVo();

        responseVo.setGameUrl(url.toString());
        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }
}