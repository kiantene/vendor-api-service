package com.nextgen.gameaggregator.vendor.spinix.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.spinix.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
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

        String agentId = credentials.get(Credentials.AGENT_ID);
        Optional.ofNullable(agentId).orElseThrow(InvalidVendorLineException::new);
        String walletType = credentials.get(Credentials.WALLET_TYPE);
        Optional.ofNullable(walletType).orElseThrow(InvalidVendorLineException::new);
        String sound = credentials.get(Credentials.SOUND);
        Optional.ofNullable(sound).orElseThrow(InvalidVendorLineException::new);
        String signatureKey = credentials.get(Credentials.SIGNATURE_KEY);
        Optional.ofNullable(signatureKey).orElseThrow(InvalidVendorLineException::new);

        Map<String, Object> arrayMap = new HashMap<>();
        arrayMap.put("platform_id", agentId);
        arrayMap.put("game_id", gameSession.getVendorGameCode());
        arrayMap.put("user_id", gameSession.getVendorPlayerUsername());
        arrayMap.put("user_token", gameSession.getToken());
        arrayMap.put("currency", gameSession.getVendorCurrencyCode());
        arrayMap.put("wallet_type", walletType);
        HashMap<String, String> settings = new HashMap<>();
        settings.put("lang", gameSession.getLanguage());
        settings.put("sd", sound);
        settings.put("eurl", gameSession.getLobbyUrl());
        arrayMap.put("settings", settings);
        String json = new Gson().toJson(arrayMap);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("json", json);
        formData.add("x_gaming_signature", VendorService.getSignature(arrayMap, signatureKey));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        String secretKey = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info("Spinix GameUrlService: " + formData.getFirst("json"));

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        GameUrlVendorResponseVo responseVo = null;

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(formData.getFirst("json"))
                .header("Authorization", secretKey)
                .header("X-Gaming-Signature", formData.getFirst("x_gaming_signature"))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(EndPoints.GAME_URL, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVendorResponseVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo.getData();
    }
}
