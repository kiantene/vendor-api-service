package com.nextgen.gameaggregator.vendor.pragmaticplay.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.GameCategories;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {
        String secureLogin = credentials.get(Credentials.SECURE_LOGIN);
        Optional.ofNullable(secureLogin).orElseThrow(InvalidVendorLineException::new);

        String secret = credentials.get(Credentials.SECRET_KEY);
        Optional.ofNullable(secret).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("secureLogin", secureLogin);
        formData.add("playerId", iBetDetailUrlInfo.getVendorUsername());

        // PP SLOT and LIVE are calling different API methods
        if (iBetDetailUrlInfo.getGameCategoryCode().equalsIgnoreCase(GameCategories.LIVE)) {
            String[] vendorGameCode = iBetDetailUrlInfo.getGameCode().split("_");
            formData.add("gameId", vendorGameCode[vendorGameCode.length - 1]);
        }

        formData.add("roundId", iBetDetailUrlInfo.getExternalRoundId());
        String hash = VendorService.generateHash(formData, secret);
        formData.add("hash", hash);

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                               IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        String apiUrl = credentials.get(Credentials.REPORT_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        // PP SLOT and LIVE are calling different API methods
        String apiPath = iBetDetailUrlInfo.getGameCategoryCode().equalsIgnoreCase(GameCategories.LIVE) ? Endpoints.OPEN_HISTORY_EXTENDED : Endpoints.OPEN_HISTORY;

        BetDetailUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        GameSession gameSession = new GameSession();

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(apiPath)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .onErrorResume(WebClientRequestException.class, e -> {
                    log.error("Failed to fetch data from {}: {}", apiUrl, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching data from " + apiUrl));
                })
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.OPEN_HISTORY, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), BetDetailUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            requestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            requestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();

        } catch (Exception exception) {
            exception.printStackTrace();
            requestService.failResponseLog(requestLogVo, exception, gameSession);
            throw new InvalidVendorResponseException();

        }

        return responseVo;
    }
}
