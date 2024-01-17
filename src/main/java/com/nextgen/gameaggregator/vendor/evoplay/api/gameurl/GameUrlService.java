package com.nextgen.gameaggregator.vendor.evoplay.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evoplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evoplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.evoplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
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

        String projId = credentials.get(Credentials.PROJ_ID);
        String key = credentials.get(Credentials.KEY);

        Optional.ofNullable(projId).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(key).orElseThrow(InvalidVendorLineException::new);

        SettingsDto settings = new SettingsDto();
        settings.setUser_id(gameSession.getVendorPlayerUsername());
        settings.setExit_url(gameSession.getLobbyUrl());
        settings.setLanguage(gameSession.getVendorLanguageCode());
        settings.setHttps(Formats.SETTINGS_HTTPS);

        GameUrlDto gameUrlDto = new GameUrlDto();
        gameUrlDto.setProject(projId);
        gameUrlDto.setVersion(Formats.VERSION);
        gameUrlDto.setToken(gameSession.getToken());
        gameUrlDto.setGame(gameSession.getVendorGameCode());
        gameUrlDto.setSettings(settings);
        gameUrlDto.setDenomination(Formats.DENOMINATION);
        gameUrlDto.setCurrency(gameSession.getVendorCurrencyCode());
        gameUrlDto.setReturn_url_info(Formats.RETURN_URL_INFO);
        gameUrlDto.setCallback_version(Formats.CALLBACK_VERSION);

        Map<String, Object> mapData = VendorService.convertObjectToMap(gameUrlDto, LinkedHashMap.class);
        VendorService.rearrangeMap(mapData);
        MultiValueMap<String, String> formData = VendorService.flattenMapIntoMultiValueMap(mapData, "");
        formData.add("signature", VendorService.md5(VendorService.buildSignature(formData, key)));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.GAME_URL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        GameUrlVo responseVo = null;

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(EndPoints.GAME_URL, uri.toString(), formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo.getData());

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }
}
