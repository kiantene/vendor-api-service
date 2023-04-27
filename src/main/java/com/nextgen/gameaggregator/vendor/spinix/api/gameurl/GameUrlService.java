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
import org.springframework.web.reactive.function.BodyInserters;
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

        String agent_id = credentials.get(Credentials.AGENT_ID);
        Optional.ofNullable(agent_id).orElseThrow(InvalidVendorLineException::new);
        String wallet_type = credentials.get(Credentials.WALLET_TYPE);
        Optional.ofNullable(wallet_type).orElseThrow(InvalidVendorLineException::new);
        String sound = credentials.get(Credentials.SOUND);
        Optional.ofNullable(sound).orElseThrow(InvalidVendorLineException::new);
        String signature_key = credentials.get(Credentials.SIGNATURE_KEY);
        Optional.ofNullable(signature_key).orElseThrow(InvalidVendorLineException::new);

        Map<String, Object> arrayMap = new HashMap<>();
        arrayMap.put("platform_id", agent_id);
        arrayMap.put("game_id", gameSession.getVendorGameCode());
        arrayMap.put("user_id", gameSession.getVendorPlayerUsername());
        arrayMap.put("user_token", gameSession.getToken());
        arrayMap.put("currency", gameSession.getVendorCurrencyCode());
        arrayMap.put("wallet_type", wallet_type);
        HashMap<String, String> settings = new HashMap<>();
        settings.put("lang", gameSession.getLanguage());
        settings.put("sd", sound);
        arrayMap.put("settings", settings);
        String json = new Gson().toJson(arrayMap);

        VendorService vendorService = new VendorService();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("json", json);
        formData.add("x_gaming_signature", vendorService.getSignature(arrayMap, signature_key));

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
        log.info("Spinix GameUrlService: " + formData.getFirst("json").toString());

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        GameUrlVendorResponseVo responseVo = null;

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(formData.getFirst("json")))
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
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVendorResponseVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            requestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            requestService.failResponseLog(requestLogVo, invalidException);
            throw new InvalidVendorResponseException();
        }

        return responseVo.getData();
    }
}
