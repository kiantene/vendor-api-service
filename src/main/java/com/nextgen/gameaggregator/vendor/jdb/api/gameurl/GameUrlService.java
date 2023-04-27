package com.nextgen.gameaggregator.vendor.jdb.api.gameurl;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.jdb.constant.Actions;
import com.nextgen.gameaggregator.vendor.jdb.constant.Credentials;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, 
        Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = gameCode.split("_");
        int gType = Integer.parseInt(parts[0]);
        String windowMode = "2";

        GameUrlDto dto = new GameUrlDto();
        dto.setAction(Actions.GAME_URL);
        dto.setTs(System.currentTimeMillis());
        dto.setParent(credentials.get(Credentials.PARENT));
        dto.setUid(gameSession.getVendorPlayerUsername());
        dto.setBalance(BigDecimal.ZERO);
        dto.setLang(gameSession.getVendorLanguageCode());
        dto.setGType(gType);
        dto.setMType(gameSession.getVendorGameCode());
        dto.setWindowMode(windowMode);

        Gson gson = new GsonBuilder().create();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        try {
            String x = VendorService.encrypt(gson.toJson(dto), credentials.get(Credentials.KEY), credentials.get(Credentials.IV));

            params.add("dc", credentials.get(Credentials.DC));
            params.add("x", x);

        }  catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }

        return params;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        String apiUrl = credentials.get(Credentials.API_SERVER);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        
        GameUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse  = WebClient.create(apiUrl)
            .post()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> Mono.empty())
            .toEntity(String.class)
            .retry(3)
            .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
            .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                "", apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException);
            throw new InvalidVendorResponseException();
        }

        return responseVo;
    }
}
