package com.nextgen.gameaggregator.vendor.whitecliff.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.Credentials;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.whitecliff.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class BetDetailService implements BetDetailUrl {


    private RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    public BetDetailService(RequestService requestService) {
        this.requestService = requestService;
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String prdId = credentials.get(Credentials.PRODUCT_ID);
        String txnId = iBetDetailUrlInfo.getExternalRoundId();
        //setup form data
        formData.add("lang", vendorLanguageCode.getLanguageCode());
        formData.add("prd_id", prdId);
        formData.add("txn_id", txnId);

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        BetDetailUrlVo responseVo;

        //BetDetails do not have game session
        GameSession gameSession = new GameSession();

        //construct API address
        String urlScheme = credentials.get(Credentials.API_URL);

        //check vendor status in our DB
        if (urlScheme == null) throw new InvalidVendorLineException();

        //game path for logging
        String apiUrl = credentials.get(Credentials.API_URL);
        String agCode = credentials.get(Credentials.AG_CODE);
        String agToken = credentials.get(Credentials.AG_TOKEN);
        String gamePath = apiUrl  + EndPoints.LAUNCH_GAME;

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();

        //Construct URI for calling to vendor API for bet details
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.BET_DETAIL_URL)
                .build()
                .encode()
                .toUri();

        //Construct header for request
        Map<String, Object> formDataMap = new HashMap<>();
        headerMap.add("ag-code", agCode);
        headerMap.add("ag-token", agToken);
        String formDataConverted = VendorService.convertMapToJson(formData);

        //Request to vendor for bet details URL
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(requestService.setHeaders(headerMap))
                .bodyValue(formDataConverted)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry()
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();

        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                gamePath, Credentials.API_URL, formDataMap, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), BetDetailUrlVo.class);

            //2. validate vendor response
            String url = responseVo.getUrl();
            if (url == null) throw new InvalidVendorResponseException();
            RequestService.validateResponse(responseVo.getUrl());

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }
        RequestService.successResponseLog(requestLogVo);

        return responseVo;

    }
}
