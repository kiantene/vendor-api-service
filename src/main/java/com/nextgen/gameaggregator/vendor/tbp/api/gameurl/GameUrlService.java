package com.nextgen.gameaggregator.vendor.tbp.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.Credentials;
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@Getter
public class GameUrlService extends BaseGameUrlService<TBPGameUrlVo> {

    public GameUrlService() {
        super(TBPGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        String token = ValidationUtils.validateCredential(credentials.get(Credentials.TOKEN));
        String username = ValidationUtils.validateCredential(credentials.get(Credentials.USERNAME));

        formData.add("PlayerToken", username);
        formData.add("PlayerName", username);
        formData.add("OperatorToken", token);
        formData.add("GameToken", gameSession.getVendorGameCode());
        formData.add("Currency", gameSession.getVendorCurrencyCode());
        formData.add("Language", gameSession.getVendorLanguageCode());
        formData.add("DefenceCode", gameSession.getToken());

        return formData;
    }

    @Override
    public TBPGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials,
                                     GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorResponseException, InvalidVendorLineException, TimeoutException {
        //construct API address
        String baseUrl = Optional.ofNullable(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);
        String launchUrl = Optional.ofNullable(credentials.get(Credentials.LAUNCH_URL))
                .orElseThrow(InvalidVendorLineException::new);

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(EndPoints.AUTHORIZE)
                .build()
                .encode()
                .toUri();

        // Trigger doPost to get session id by calling vendor api
        ResponseEntity<String> apiResponse = this.doPost(baseUrl, uri.toString(), new HttpHeaders(), formData, isTimeout);

        this.validateResponse(apiResponse, isTimeout, httpRequestLog, TBPGameUrlVo.class, gameSession);

        TBPGameUrlVo responseVo = new Gson().fromJson(apiResponse.getBody(), TBPGameUrlVo.class);

        String gameurl = launchUrl + gameSession.getVendorGameCode();
        responseVo.setGameUrl(gameurl);
        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }
}
