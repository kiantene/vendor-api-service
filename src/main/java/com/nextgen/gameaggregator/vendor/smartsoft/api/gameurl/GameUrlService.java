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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;


public class GameUrlService extends BaseGameUrlService<SSGameUrlVo> {

    public GameUrlService() {
        super(SSGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        String portalName = ValidationUtils.validateCredential(credentials.get(Credentials.PORTAL_NAME));

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

        URI url = UriComponentsBuilder.fromUriString(launchUrl)
                .path(EndPoints.LAUNCH_GAME)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        SSGameUrlVo responseVo = new SSGameUrlVo();

        responseVo.setGameUrl(url.toString());
        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }
}