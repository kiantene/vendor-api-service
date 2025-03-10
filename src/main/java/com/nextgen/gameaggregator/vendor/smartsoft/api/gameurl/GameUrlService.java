package com.nextgen.gameaggregator.vendor.smartsoft.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.wmlive.api.gameurl.WMGameUrlVo;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


public class GameUrlService extends BaseGameUrlService<SSGameUrlVo> {

    protected GameUrlService(Class<SSGameUrlVo> responseVoClass) {
        super(responseVoClass);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

//        this.portalName = ValidationUtils.validateCredential(credentials.get(Credentials.PORTAL_NAME));
//
//        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//        formData.add("GameName", gameSession.getG);
//        formData.add("Token", gameSession.getToken());
//        formData.add("ReturnUrl", gameSession.getLobbyUrl());
//        formData.add("Lang", gameSession.getLanguage());
//        formData.add("PortalName", gameSession.getVendorPlayerUsername());

        return formData;
    }

    @Override
    public WMGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                    GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, TimeoutException {
        AtomicBoolean isTimeout = new AtomicBoolean(false);

        HttpHeaders httpHeaders = this.getHeaders(new HttpHeaders(), formData, credentials, gameSession);

//        URI urlScheme = UriComponentsBuilder.fromUriString(this.getLaunchUrl())
//                .queryParams(formData)
//                .build()
//                .encode()
//                .toUri();
//
//        // Trigger doPost to get game url function by calling vendor api
//        ResponseEntity<String> response = this.doPost(this.getLaunchUrl(), urlScheme.toString(), httpHeaders, formData, isTimeout);
//
//        this.validateResponse(response, isTimeout, httpRequestLog, SSGameUrlVo.class, gameSession);
//
//        WMGameUrlVo responseVo = new Gson().fromJson(response.getBody(), SSGameUrlVo.class);
//
//        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }
}