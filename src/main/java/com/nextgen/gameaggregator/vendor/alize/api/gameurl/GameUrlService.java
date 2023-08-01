package com.nextgen.gameaggregator.vendor.alize.api.gameurl;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.alize.constant.Credentials;
import com.nextgen.gameaggregator.vendor.alize.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alize.service.VendorService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Autowired
    RequestService requestService;
    @Autowired
    VendorLineService vendorLineService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
            Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        // Get operator by vendor line
        String operator = "";
        try {
            operator = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator");
        } catch (CredentialNotFoundException e) {
            log.error("Credential not found : " + e.getMessage());
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("player", gameSession.getVendorPlayerUsername());
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("gamecode", gameSession.getVendorGameCode());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("ip", gameSession.getIpAddress());
        formData.add("operator", operator);
        formData.add("playmode", "free");
        formData.add("timestamp", String.valueOf(System.currentTimeMillis()));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
            GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {

        // Retrieve the API URL and key from the credentials map
        String apiUrl = credentials.get(Credentials.API_URL);
        String apiKey = credentials.get(Credentials.API_KEY);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);

        GameUrlVo responseVo = new GameUrlVo();

        // Generate the signature with the API secret and form data
        String apiSecret = credentials.get(Credentials.SECRET_KEY);
        String signatureBody = this.getSignatureBody(formData, apiKey);
        Optional.ofNullable(apiSecret).orElseThrow(InvalidVendorLineException::new);
        String signature = VendorService.generateHash(apiSecret, signatureBody);

        // Define headers for the request
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headerMap.add("X-API-Key", apiKey);
        headerMap.add("X-Signature", signature);

        Long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.GAME_URL)
                .headers(header -> header.addAll(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
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
            responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

            // 2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }

    private String getSignatureBody(MultiValueMap<String, String> formData, String apiKey) {
        Map<String, String> signatureBodyMap = new LinkedHashMap<>();
        signatureBodyMap.put("apikey", apiKey);
        signatureBodyMap.put("gamecode", formData.getFirst("gamecode"));
        signatureBodyMap.put("player", formData.getFirst("player"));
        signatureBodyMap.put("currency", formData.getFirst("currency"));
        signatureBodyMap.put("ip", formData.getFirst("ip"));
        signatureBodyMap.put("lang", formData.getFirst("lang"));

        return new Gson().toJson(signatureBodyMap);
    }
}
