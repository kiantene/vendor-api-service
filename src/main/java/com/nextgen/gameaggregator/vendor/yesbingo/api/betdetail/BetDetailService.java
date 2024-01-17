package com.nextgen.gameaggregator.vendor.yesbingo.api.betdetail;

import com.couchbase.client.core.deps.com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yesbingo.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class BetDetailService implements BetDetailUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode) throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        String apiUrl = credentials.get(com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        String aesKey = credentials.get(com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials.AES_KEY);
        Optional.ofNullable(aesKey).orElseThrow(InvalidVendorLineException::new);
        String aesIv = credentials.get(com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials.AES_IV);
        Optional.ofNullable(aesIv).orElseThrow(InvalidVendorLineException::new);
        String dc = credentials.get(com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials.DC);
        Optional.ofNullable(dc).orElseThrow(InvalidVendorLineException::new);
        String agent = credentials.get(com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials.AGENT);
        Optional.ofNullable(agent).orElseThrow(InvalidVendorLineException::new);

        JsonObject params = new JsonObject();
        String encrypted = "";

        try {

            long unixTimestamp = Instant.now().toEpochMilli();
            String[] parts = iBetDetailUrlInfo.getGameCode().split("_");
            String gType = parts[1];

            params.addProperty("action", EndPoints.BET_DETAIL);
            params.addProperty("ts", unixTimestamp);
            params.addProperty("uid", iBetDetailUrlInfo.getVendorUsername());
            params.addProperty("parent", agent);
            params.addProperty("lang", vendorLanguageCode.getLanguageCode());
            params.addProperty("gType", gType);
            params.addProperty("seqNo", iBetDetailUrlInfo.getExternalRoundId());

            encrypted = VendorService.encrypt(params.toString(), aesKey, aesIv);

        } catch (Exception exception) {
            throw new InvalidVendorLineException();
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.set("json", params.toString());
        formData.set("dc", dc);
        formData.set("encrypted", encrypted);
        formData.set("apiUrl", apiUrl);

        return formData;
    }

    @Override
    public com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode) throws InvalidVendorResponseException, InvalidVendorLineException {

        String apiUrl = formData.getFirst("apiUrl");
        String requestBody = "dc=" + formData.getFirst("dc") + "&x=" + formData.getFirst("encrypted");

        BetDetailUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        //BetDetails do not have game session;
        GameSession gameSession = new GameSession();

        long startTime = System.currentTimeMillis();
        ResponseEntity apiResponse = WebClient.create()
                .post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(apiUrl, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
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
