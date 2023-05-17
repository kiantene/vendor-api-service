package com.nextgen.gameaggregator.vendor.pragmaticplay.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        String secureLogin = credentials.get(Credentials.SECURE_LOGIN);
        Optional.ofNullable(secureLogin).orElseThrow(InvalidVendorLineException::new);

        String secret = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secret).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secureLogin", secureLogin);
        formData.add("symbol", gameSession.getVendorGameCode());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("technology", "H5");
        formData.add("token", gameSession.getToken());
        formData.add("platform", gameSession.getVendorPlatformCode());
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("lobbyUrl", gameSession.getLobbyUrl());
        String hash = VendorService.generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        GameUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        Long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.GAME_URL )
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        Long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.GAME_URL, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            requestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            requestService.failResponseLog(requestLogVo, invalidException);
            throw new InvalidVendorResponseException();
        }

        return responseVo;
    }
}
