package com.nextgen.gameaggregator.vendor.joker.api.betdetail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.joker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.joker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.joker.constant.Formats;
import com.nextgen.gameaggregator.vendor.joker.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
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

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        VendorService vendorService = new VendorService();

        //convert round id into vendor format (round id in db is in "username_roundId" format)
        String roundID = "";
        if (iBetDetailUrlInfo.getExternalRoundId().startsWith(iBetDetailUrlInfo.getVendorUsername().toUpperCase())) {
            roundID = iBetDetailUrlInfo.getExternalRoundId().substring(iBetDetailUrlInfo.getVendorUsername().length() + 1);
        }

        //convert game code into vendor format
        String gameCode = "";
        if (iBetDetailUrlInfo.getGameCode().startsWith("joker_")) {
            gameCode = iBetDetailUrlInfo.getGameCode().substring("joker_".length());
        }

        //setup form data
        formData.add("AppID", credentials.get(Credentials.APP_ID));
        formData.add("Username", iBetDetailUrlInfo.getVendorUsername().toUpperCase());
        formData.add("GameCode", gameCode);
        formData.add("RoundID", roundID);
        formData.add("Language", vendorLanguageCode.getLanguageCode());
        formData.add("Timestamp", String.valueOf(System.currentTimeMillis()));
        String hash = VendorService.generateHash(formData, credentials.get(Credentials.SECRET));
        formData.add("Hash", hash);

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                               IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + EndPoints.BET_DETAIL_URL);
        log.info(formData.toString());

        //convert from data into hashmap data
        Map<String, Object> convertFormMap = new LinkedHashMap<>();
        convertFormMap.put("AppID", formData.getFirst("AppID"));
        convertFormMap.put("Username", formData.getFirst("Username"));
        convertFormMap.put("GameCode", formData.getFirst("GameCode"));
        convertFormMap.put("RoundID", formData.getFirst("RoundID"));
        convertFormMap.put("Timestamp", formData.getFirst("Timestamp"));
        convertFormMap.put("Language", formData.getFirst("Language"));
        convertFormMap.put("Hash", formData.getFirst("Hash"));

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.BET_DETAIL_URL)
                .build()
                .encode()
                .toUri();

        BetDetailUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, Formats.APPLICATION_JSON);
        headerMap.add(HttpHeaders.ACCEPT, Formats.APPLICATION_JSON);

        //post request to vendor API with JSON string
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .bodyValue(new Gson().toJson(convertFormMap))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(Formats.RETRY)
                .timeout(Duration.ofMillis(Formats.TIMEOUT))
                .block();


        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.BET_DETAIL_URL, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = HttpService.convertJsonToDto(String.valueOf(apiResponse.getBody()), BetDetailUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(() -> new InvalidVendorResponseException());
            requestService.validateResponse(responseVo);

            requestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException | JsonProcessingException invalidException) {
            requestService.failResponseLog(requestLogVo, invalidException);
            throw new InvalidVendorResponseException();
        }

        return responseVo;
    }
}
