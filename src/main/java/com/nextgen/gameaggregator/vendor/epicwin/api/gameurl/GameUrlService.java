package com.nextgen.gameaggregator.vendor.epicwin.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.epicwin.constant.Credentials;
import com.nextgen.gameaggregator.vendor.epicwin.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.epicwin.constant.Formats;
import com.nextgen.gameaggregator.vendor.epicwin.service.VendorService;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Service
public class GameUrlService implements GameUrl {
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private RequestService requestService;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException {

        // Get the current date and time in UTC
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of(Formats.TIME_ZONE));
        // Define the formatter for the desired output pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_FORMAT);
        // Format the current date and time using the formatter
        String formattedDateTime = currentDateTime.format(formatter);

        String functionName = "GameLogin";
        String requestDateTime = formattedDateTime;
        String playerId = gameSession.getVendorPlayerUsername();

        String operatorId = credentials.get(Credentials.OPERATOR_ID);
        Optional.ofNullable(operatorId).orElseThrow(InvalidVendorLineException::new);

        String secretKey = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secretKey).orElseThrow(InvalidVendorLineException::new);

        //generate encryptString
        String encryptString = functionName + requestDateTime + operatorId + secretKey + playerId;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("OperatorId", operatorId);
        formData.add("RequestDateTime", requestDateTime);
        formData.add("PlayerId", playerId);
        formData.add("Ip", gameSession.getIpAddress());
        formData.add("GameCode", gameSession.getVendorGameCode());
        formData.add("Currency", gameSession.getVendorCurrencyCode());
        formData.add("Lang", gameSession.getVendorLanguageCode());
        formData.add("RedirectUrl", gameSession.getLobbyUrl());
        formData.add("AuthToken", gameSession.getToken());

        //hash all the data to generate sign value
        formData.add("Signature", vendorService.generateSign(encryptString));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        String urlScheme = credentials.get(Credentials.GAME_URL);
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        GameUrlVo responseVo = null;

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add("Content-Type", "application/json");
        String formdataJSON = vendorService.convertMapToJson(formData);

        String gamePath = EndPoints.GAME_URL;
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(gamePath)
                .build()
                .encode()
                .toUri();

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
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

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                gamePath, urlScheme, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }
        return responseVo;
    }
}
