package com.nextgen.gameaggregator.vendor.dotconnections.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dotconnections.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dotconnections.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;
    @Autowired
    VendorService vendorService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode) throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        String brandId = credentials.get(Credentials.BRAND_ID);
        Optional.ofNullable(brandId).orElseThrow(InvalidVendorLineException::new);
        String apiKey = credentials.get(Credentials.API_KEY);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);
        String provider = credentials.get(Credentials.PROVIDER_CODE);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);

        String roundId = iBetDetailUrlInfo.getExternalRoundId();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.set("brand_id", brandId);
        formData.set("sign", VendorService.getSign(brandId + roundId + apiKey));
        formData.set("brand_uid", iBetDetailUrlInfo.getVendorUsername());
        formData.set("currency", iBetDetailUrlInfo.getVendorCurrencyCode());
        formData.set("provider", provider);
        formData.set("round_id", roundId);

        return formData;
    }

    @Override
    public com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode) throws InvalidVendorResponseException, InvalidVendorLineException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        Map<String, String> map = formData.toSingleValueMap();
        String json = new Gson().toJson(map);

        BetDetailUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        //bet details do not have player session;
        GameSession gameSession = new GameSession();

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_RESULT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(EndPoints.GAME_RESULT, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
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
        }

        return responseVo;
    }
}
