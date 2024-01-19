package com.nextgen.gameaggregator.vendor.mg.api.gameurl;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.mg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.mg.service.VendorTokenService;

import reactor.core.publisher.Mono;

public class GameUrlService implements GameUrl {
    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorTokenService vendorTokenService;

    @Value("${spring.profiles.active}")
    private String profilesActive;
    
    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
            Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            try {
                formData.add("contentCode", gameSession.getVendorGameCode());
                formData.add("platform", gameSession.getVendorPlatformCode());
                formData.add("langCode", gameSession.getVendorLanguageCode());

            }  catch (Exception exception) {
                throw new InvalidFormatException(exception.getMessage());
            }
        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
            GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {

            String apiUrl = credentials.get(Credentials.API_URL)
                            + "/agents/" + credentials.get(Credentials.AGENT_CODE)
                            + "/players/" + gameSession.getVendorPlayerUsername()
                            + "/sessions";
            Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

            String token = vendorTokenService.getToken(gameSession.getVendorLineId());
            
            GameUrlVo responseVo = null;
            MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
            headerMap.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headerMap.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            long startTime = System.currentTimeMillis();

            ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(apiUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headerMap))
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

            long endTime = System.currentTimeMillis();
            RequestLogVo requestLogVo = requestService.createRequestLogVo(
                "", apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

            try {
                // 1. validate HTTP Response Code
                requestService.validateVendorHttpStatusResponse(apiResponse);
                responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

                //2. validate vendor response
                Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
                RequestService.validateResponse(responseVo);
                RequestService.successResponseLog(requestLogVo);

            } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
                RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
                String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
                throw new InvalidVendorResponseException(exceptionMsg);
            }

        return responseVo;
    }
    
}
