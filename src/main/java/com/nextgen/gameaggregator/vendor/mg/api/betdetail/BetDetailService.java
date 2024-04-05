package com.nextgen.gameaggregator.vendor.mg.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.mg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mg.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.mg.service.VendorTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    private RequestService requestService;
    @Autowired
    private VendorTokenService vendorTokenService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials,
                                                         IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        try {
            formData.add("utcOffset", "0");
            formData.add("betId", iBetDetailUrlInfo.getExternalRoundId());
            formData.add("langCode", vendorLanguageCode.getLanguageCode());

        } catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                               IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        String apiUrl = credentials.get(Credentials.API_URL)
                + "/agents/" + credentials.get(Credentials.AGENT_CODE)
                + "/players/" + iBetDetailUrlInfo.getVendorUsername()
                + "/betVisualizers";
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        String token = vendorTokenService.getToken(iBetDetailUrlInfo.getVendorLineId());

        BetDetailUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        headerMap.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(apiUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headerMap))
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                "", apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), BetDetailUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            GameSession gameSession = new GameSession();
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
        }

        return responseVo;
    }

}
