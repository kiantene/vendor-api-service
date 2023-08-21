package com.nextgen.gameaggregator.vendor.evolution.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.evolution.constant.Credentials;
import com.nextgen.gameaggregator.vendor.evolution.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolution.service.VendorService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        return new LinkedMultiValueMap<>();
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        String casinoKey = credentials.get(Credentials.CASINO_KEY);
        String authToken = credentials.get(Credentials.AUTH_TOKEN);
        String countryCode = credentials.get(Credentials.COUNTRY_CODE);

        // Check Credentials Null
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(casinoKey).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(authToken).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(countryCode).orElseThrow(InvalidVendorLineException::new);

        // set DTO
        PlayerSessionDto playerSessionDto = vendorService.setPlayerSessionDto(gameSession);
        PlayerDto playerDto = vendorService.setPlayerDto(gameSession, playerSessionDto, countryCode);
        ConfigChannelDto configChannelDto = vendorService.setConfigChannelDto(gameSession);
        GameTableDto gameTableDto = vendorService.setGameTableDto(gameSession);
        ConfigGameDto configGameDto = vendorService.setConfigGameDto(gameTableDto);
        ConfigDto configDto = vendorService.setConfigDto(configGameDto, configChannelDto);

        Map<String, Object> formDataMap = new HashMap<>();
        formDataMap.put("uuid", UUID.randomUUID().toString());
        formDataMap.put("player", playerDto);
        formDataMap.put("config", configDto);

        String gamePath = EndPoints.GAME_PATH + casinoKey + "/" + authToken;
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(gamePath)
                .build()
                .encode()
                .toUri();

        GameUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
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
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVo.class);
            responseVo.setHostName(apiUrl);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            requestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            requestService.failResponseLog(requestLogVo, invalidException);
            throw new InvalidVendorResponseException();
        }


        return responseVo;

    }
}
