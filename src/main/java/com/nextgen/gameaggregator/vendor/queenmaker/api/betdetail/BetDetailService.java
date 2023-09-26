package com.nextgen.gameaggregator.vendor.queenmaker.api.betdetail;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BetDetailService implements BetDetailUrl {
    @Autowired
    RequestService requestService;
    @Autowired
    VendorService vendorService;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        return new LinkedMultiValueMap<>();
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {
        String clientId = credentials.get(Credentials.CLIENT_ID);
        String clientSecret = credentials.get(Credentials.CLIENT_SECRET);
        String apiUrl = credentials.get(Credentials.API_URL);

        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = vendorService.splitGameCode(iBetDetailUrlInfo.getGameCode(), 3);
        String gpcode = parts[1];
        
        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.HISTORY)
                .pathSegment(gpcode)
                .path("/rounds")
                .pathSegment(iBetDetailUrlInfo.getExternalRoundId())
                .path("/users")
                .pathSegment(iBetDetailUrlInfo.getVendorUsername())
                .queryParam("lang", vendorLanguageCode.getLanguageCode())
                .build()
                .encode()
                .toUri();

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add(Formats.HEADER_CLIENT_ID, clientId);
        headerMap.add(Formats.HEADER_CLIENT_SECRET, clientSecret);

        long startTime = System.currentTimeMillis();
        BetDetailUrlVo responseVo = null;
        ResponseEntity<String> apiResponse = WebClient.create()
                .get()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.HISTORY, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), BetDetailUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            requestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            requestService.failResponseLog(requestLogVo, invalidException);
            throw new InvalidVendorResponseException();

        } catch (Exception exception) {
            exception.printStackTrace();
            requestService.failResponseLog(requestLogVo, exception);
            throw new InvalidVendorResponseException();

        }

        return responseVo;
    }
}
