package com.nextgen.gameaggregator.vendor.spadegaming.api.betdetail;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.EndPoints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials,
                                                         IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        String merchantCode = ValidationUtils.validateCredential(credentials.get(Credentials.MERCHANT_CODE));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("merchantCode", merchantCode);
        formData.add("acctId", iBetDetailUrlInfo.getVendorUsername());
        formData.add("ticketId", iBetDetailUrlInfo.getVendorBetId());
        formData.add("action", "ticketLog");
        formData.add("serialNo", UUID.randomUUID().toString());

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                               IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        BetDetailUrlVo responseVo;
        // Retrieve the game domain from the credentials map.
        String vendorApiUrl = ValidationUtils.validateCredential(credentials.get(Credentials.VENDOR_API_URL));

        // Build the URI needed to call the Spadegaming game URL API.
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add("API", "getTicketLog");
        headerMap.add("DataType", "JSON");
        headerMap.add("Digest", "");

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create(vendorApiUrl)
                .post()
                .headers(requestService.setHeaders(headerMap))
                .bodyValue(formData.toSingleValueMap())
                .retrieve()
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .onErrorResume(TimeoutException.class, Mono::error)
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                "", vendorApiUrl, formData.toSingleValueMap(), apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            apiResponse = Optional.ofNullable(apiResponse).orElseThrow(InvalidResponseException::new);
            requestService.validateVendorHttpStatusResponse(apiResponse);

            //2. validate vendor response
            responseVo = Optional.ofNullable(new Gson().fromJson(apiResponse.getBody(), BetDetailUrlVo.class))
                    .orElseThrow(InvalidVendorResponseException::new);

            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);
        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {

            GameSession gameSession = new GameSession();
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }
        // Create a new GameUrlVo object and set the game URL as its value.
        return responseVo;
    }
}
