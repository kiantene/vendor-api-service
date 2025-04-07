package com.nextgen.gameaggregator.vendor.avatarux.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Credentials;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@Getter
public class GameUrlService extends BaseGameUrlService<AUGameUrlVo> {

    public GameUrlService() {
        super(AUGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String apiName = ValidationUtils.validateCredential(credentials.get(Credentials.API_NAME));

        formData.add("provider", "avatarux");
        formData.add("wallet", apiName);
        formData.add("operator", gameSession.getAgentPlayerUsername());
        formData.add("game", gameSession.getVendorGameCode());
        formData.add("key", gameSession.getToken());

        return formData;
    }

    @Override
    public AUGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                    GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, InvalidVendorLineException, TimeoutException {
        //construct API address
        String launchUrl = Optional.ofNullable(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);

        URI url = UriComponentsBuilder.fromUriString(launchUrl)
                .path(EndPoints.LAUNCHER)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        AUGameUrlVo responseVo = new AUGameUrlVo();

        responseVo.setGameUrl(url.toString());
        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }
}
