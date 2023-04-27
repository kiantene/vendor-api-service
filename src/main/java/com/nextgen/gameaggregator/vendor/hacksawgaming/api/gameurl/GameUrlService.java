package com.nextgen.gameaggregator.vendor.hacksawgaming.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.hacksawgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.hacksawgaming.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String brandId = credentials.get(Credentials.BRAND_ID);
        Optional.ofNullable(brandId).orElseThrow(InvalidVendorLineException::new);
        String apiKey = credentials.get(Credentials.API_KEY);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);
        String countryCode = credentials.get(Credentials.COUNTRY_CODE);
        Optional.ofNullable(countryCode).orElseThrow(InvalidVendorLineException::new);

        String brandUid = gameSession.getVendorPlayerUsername();
        String platform = "pc";
        if(!gameSession.getPlatformId().equals(2)) {
            platform = "mobile";
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.set("brand_id", brandId);
        formData.set("sign", VendorService.getSign(brandId + brandUid + apiKey));
        formData.set("brand_uid", brandUid);
        formData.set("token", VendorService.removeDashes(gameSession.getToken()));
        formData.set("game_id", gameSession.getVendorGameCode());
        formData.set("currency", gameSession.getVendorCurrencyCode());
        formData.set("language", gameSession.getVendorLanguageCode());
        formData.set("channel", platform);
        formData.set("country_code", countryCode);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Map<String, String> map = formData.toSingleValueMap();
        String json = new Gson().toJson(map);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info("HSG GameUrlService: " + json);

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        GameUrlVendorResponseVo responseVo = null;

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_URL)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(json)
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
