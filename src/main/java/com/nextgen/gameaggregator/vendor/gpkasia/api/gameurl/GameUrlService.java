package com.nextgen.gameaggregator.vendor.gpkasia.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Platforms;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    VendorService vendorService;

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("api_token", credentials.get(Credentials.api_token));
        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("password", gameSession.getVendorPlayerUsername());
        formData.add("platform", credentials.get(Credentials.platform_id));
        formData.add("timestamp", String.valueOf(vendorService.getCurrentTime()));
        formData.add("mode", gameSession.getVendorGameCode());
        formData.add("home_url", gameSession.getLobbyUrl());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("client_type", Platforms.checkPlatformCode(gameSession.getVendorPlatformCode()));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException{
        GameUrlVo responseVo = new GameUrlVo();

        //construct API address
        String urlScheme = credentials.get(Credentials.api_url);

        //check vendor status in our DB
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        // convert multi value map into hash map for login game
        Map<String, Object> loginGame = vendorService.convertToHashMap(formData);

        // convert platform id from string into int
        loginGame.put("platform", Integer.parseInt((String) loginGame.get("platform")));

        // convert timestamp from string into int
        loginGame.put("timestamp", Long.parseLong((String) loginGame.get("timestamp")));

        Map<String, Object> createPlayer = new HashMap<>();

        createPlayer.put("api_token", loginGame.get("api_token"));
        createPlayer.put("user", loginGame.get("user"));
        createPlayer.put("password", loginGame.get("user"));
        createPlayer.put("username", loginGame.get("user"));
        createPlayer.put("currency", gameSession.getVendorCurrencyCode());
        createPlayer.put("platform", loginGame.get("platform"));
        createPlayer.put("timestamp", loginGame.get("timestamp"));

        long startTime = System.currentTimeMillis();

        // Convert HashMap to JSON string using Gson
        Gson gson = new Gson();
        String jsonString = gson.toJson(createPlayer);

        // Trigger create member function by calling vendor api
        ResponseEntity<String> apiResponse = createMember(urlScheme, jsonString);

        long endTime = System.currentTimeMillis();

        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.CREATE_PLAYER, urlScheme, jsonString, apiResponse, null, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        long startTime2 = System.currentTimeMillis();

        // Convert HashMap to JSON string using Gson
        Gson gson2 = new Gson();
        String jsonString2 = gson2.toJson(loginGame);

        // request to get game url through vendor api
        ResponseEntity<String> apiResponse2 = getGameUrl(urlScheme, jsonString2);

        long endTime2 = System.currentTimeMillis();

        RequestLogVo requestLogVo2 = requestService.createRequestLogVo(
                EndPoints.LAUNCH_GAME, urlScheme, jsonString2, apiResponse, null, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try{
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse2);
            responseVo = new Gson().fromJson((String) apiResponse2.getBody(), GameUrlVo.class);

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

    private ResponseEntity<String> createMember(String urlScheme, String createPlayer){
        //Construct the API to register player from vendor site
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.CREATE_PLAYER)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .bodyValue(createPlayer)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        return apiResponse;
    }

    private ResponseEntity<String> getGameUrl(String urlScheme, String loginGame){
        //Construct the API to register player from vendor site
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.LAUNCH_GAME)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .bodyValue(loginGame)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        return apiResponse;
    }
}
