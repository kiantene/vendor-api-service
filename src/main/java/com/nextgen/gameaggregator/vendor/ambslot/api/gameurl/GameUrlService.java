package com.nextgen.gameaggregator.vendor.ambslot.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.ambslot.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ambslot.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ambslot.service.VendorService;
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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
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
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        // trim game code by removing "_stg" or "_STG"
        String vendorGameCode = VendorService.trimGameCode(gameSession.getVendorGameCode());

        formData.add("username", gameSession.getVendorPlayerUsername());
        formData.add("gameId", vendorGameCode);
        formData.add("backLink", gameSession.getLobbyUrl());
        formData.add("agent", credentials.get(Credentials.prefix));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        //construct API address
        String urlScheme = credentials.get(Credentials.api_url);

        //check vendor status in our DB
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        MultiValueMap<String, String> registerData = new LinkedMultiValueMap<>();

        // Assign value for create player
        registerData.add("username", gameSession.getVendorPlayerUsername());
        registerData.add("password", gameSession.getVendorPlayerUsername());
        registerData.add("agent", credentials.get(Credentials.prefix));

        // Convert map into string format as json
        String registerDataJSON = VendorService.convertMapToJson(registerData);
        String formdataJSON = VendorService.convertMapToJson(formData);

        String secret = credentials.get(Credentials.secret);
        int iterations = 1000;

        // Generate x-ambslot-signature value for create member
        String encryptedValue = VendorService.encryption(registerDataJSON, secret, iterations);

        // Generate generate x-ambslot-signature value for login game
        String encryptedValue2 = VendorService.encryption(formdataJSON, secret, iterations);

        // Assign value for header
        headerMap.add("x-ambslot-signature", encryptedValue);

//        long startTime = System.currentTimeMillis();

        // Trigger create member function by calling vendor api
        ResponseEntity<String> apiResponse = createMember(urlScheme, headerMap, registerDataJSON);

//        long endTime = System.currentTimeMillis();

//        RequestLogVo requestLogVo = requestService.createRequestLogVo(
//                EndPoints.CREATE_PLAYER, urlScheme, registerData, apiResponse, headerMap, startTime, endTime,
//                this.getClass().getPackage().getName(), profilesActive);

        long startTime2 = System.currentTimeMillis();

        MultiValueMap<String, String> headerMap2 = new LinkedMultiValueMap<>();

        // Assign value for header
        headerMap2.add("Accept-Language", gameSession.getVendorLanguageCode());
        headerMap2.add("x-ambslot-signature", encryptedValue2);

        // generate game url
        ResponseEntity<String> apiResponse2 = launchGame(urlScheme, headerMap2, formdataJSON);

        long endTime2 = System.currentTimeMillis();

        RequestLogVo requestLogVo2 = requestService.createRequestLogVo(
                EndPoints.LAUNCH_GAME, urlScheme, formData, apiResponse2, headerMap2, startTime2, endTime2,
                this.getClass().getPackage().getName(), profilesActive);

        GameUrlVo responseVo = null;

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse2);
            responseVo = new Gson().fromJson(apiResponse2.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

//            RequestService.successResponseLog(requestLogVo);
            RequestService.successResponseLog(requestLogVo2);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
//            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            RequestService.failResponseLog(requestLogVo2, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }

    private ResponseEntity<String> createMember(String urlScheme, MultiValueMap<String, String> headerMap, String registerDataJSON){
        //Construct the API to register player from vendor site
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.CREATE_PLAYER)
                .build()
                .encode()
                .toUri();

        return WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerDataJSON)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();
    }

    private ResponseEntity<String> launchGame(String urlScheme, MultiValueMap<String, String> headerMap, String formdataJSON){
        //Construct the API to register player from vendor site
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.LAUNCH_GAME)
                .build()
                .encode()
                .toUri();
        
        return WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(formdataJSON)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();
    }
}
