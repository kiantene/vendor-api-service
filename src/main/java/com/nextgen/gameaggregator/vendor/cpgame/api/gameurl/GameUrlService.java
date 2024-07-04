package com.nextgen.gameaggregator.vendor.cpgame.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.cpgame.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cpgame.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cpgame.constant.GameKey;
import com.nextgen.gameaggregator.vendor.cpgame.service.VendorService;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;

    @Autowired
    VendorService vendorService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("userName", gameSession.getVendorPlayerUsername());
        formData.add("language", gameSession.getVendorLanguageCode());
        formData.add("gameId", gameSession.getVendorGameCode());
        formData.add("subUid", String.valueOf(gameSession.getVendorPlayerId()));
        formData.add("secretKey", credentials.get(Credentials.secret_key));
        formData.add("appId", credentials.get(Credentials.app_id));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        //construct API address
        String urlScheme = credentials.get(Credentials.api_url);

        //check vendor status in our DB
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        // convert MultiValueMap into hash map
        Map<String, Object> hashMap = vendorService.convertMultiValueMapToHashMap(formData);
        hashMap.put("subUid", Integer.parseInt((String) hashMap.get("subUid")));

        // define time for response data to vendor
        long currentTimeMillis = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(currentTimeMillis);

        // define data for register member
        Map<String, Object> registerMember = new HashMap<>();
        registerMember.put("time", instant.getEpochSecond());
        registerMember.put("user_name", hashMap.get("userName"));
        registerMember.put("sub_uid", hashMap.get("subUid"));
        registerMember.put("game_key", GameKey.gameKey);
        registerMember.put("appid", hashMap.get("appId"));
        registerMember.put("token", VendorService.generateToken(registerMember, (String) hashMap.get("secretKey")));

        // Trigger create member function by calling vendor api
        ResponseEntity<String> apiResponse = createMember(urlScheme, registerMember);

        // define data for get game url
        Map<String, Object> launchGame = new HashMap<>();
        launchGame.put("appid", hashMap.get("appId"));
        launchGame.put("game_key", GameKey.gameKey);
        launchGame.put("sub_uid", hashMap.get("subUid"));
        launchGame.put("game_id", hashMap.get("gameId"));
        launchGame.put("lang", hashMap.get("language"));
        launchGame.put("time", instant.getEpochSecond());
        launchGame.put("token", VendorService.generateToken(launchGame, (String) hashMap.get("secretKey")));

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        long startTime = System.currentTimeMillis();

        // Trigger get game url function by calling vendor api
        ResponseEntity<String> apiResponse2 = getGameURL(urlScheme, launchGame);

        long endTime = System.currentTimeMillis();

        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.LAUNCH_GAME, urlScheme, formData, apiResponse2, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        GameUrlVo responseVo = new GameUrlVo();

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse2);
            responseVo = new Gson().fromJson((String) apiResponse2.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
//            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }

    private ResponseEntity<String> createMember(String urlScheme, Map<String, Object> map) {
        //Construct the API to register player from vendor site
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.CREATE_PLAYER)
                .build()
                .encode()
                .toUri();

        Gson gson = new Gson();

        return WebClient.create()
                .post()
                .uri(uri)
//                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gson.toJson(map).toString())
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();
    }

    private ResponseEntity<String> getGameURL(String urlScheme, Map<String, Object> map) {
        //Construct the API to register player from vendor site
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.LAUNCH_GAME)
                .build()
                .encode()
                .toUri();

        Gson gson = new Gson();

        return WebClient.create()
                .post()
                .uri(uri)
//                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gson.toJson(map).toString())
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();
    }
}
