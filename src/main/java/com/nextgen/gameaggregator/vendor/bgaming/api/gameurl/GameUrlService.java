package com.nextgen.gameaggregator.vendor.bgaming.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.bgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bgaming.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class GameUrlService implements GameUrl {
    @Autowired
    VendorService vendorService;
    @Autowired
    RequestService requestService;
    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    private HttpService httpService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        return new LinkedMultiValueMap<>();
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {
        String apiUrl = credentials.get(Credentials.GCP_URL);
        String casinoId = credentials.get(Credentials.CASINO_ID);
        String authToken = credentials.get(Credentials.AUTH_TOKEN);

        // Check Credentials Null
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(casinoId).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(authToken).orElseThrow(InvalidVendorLineException::new);

        UserDto userDto = vendorService.setUserDto(gameSession);

        Map<String, Object> formDataMap = new HashMap<>();
        formDataMap.put("casino_id", casinoId);
        formDataMap.put("game", gameSession.getVendorGameCode());
        formDataMap.put("currency", gameSession.getVendorCurrencyCode());
        formDataMap.put("locale", gameSession.getVendorLanguageCode());
        formDataMap.put("client_type", gameSession.getVendorPlatformCode());
        formDataMap.put("user", userDto);

        String signature = vendorService.generateSign(authToken, new Gson().toJson(formDataMap));

        String gamePath = EndPoints.SESSIONS;
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(gamePath)
                .build()
                .encode()
                .toUri();

        GameUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add("X-REQUEST-SIGN", signature);

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Gson().toJson(formDataMap))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                gamePath, apiUrl, formDataMap, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = HttpService.convertJsonToDto(apiResponse.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException |
                 JsonProcessingException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }
        return responseVo;
    }
}