package com.nextgen.gameaggregator.vendor.yesbingo.api.gameurl;

import com.couchbase.client.core.deps.com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.Credentials;
import com.nextgen.gameaggregator.vendor.yesbingo.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.yesbingo.service.VendorService;
import lombok.extern.slf4j.Slf4j;
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
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;


@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;

    @Autowired
    private WalletService walletService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        String aesKey = credentials.get(Credentials.AES_KEY);
        Optional.ofNullable(aesKey).orElseThrow(InvalidVendorLineException::new);
        String aesIv = credentials.get(Credentials.AES_IV);
        Optional.ofNullable(aesIv).orElseThrow(InvalidVendorLineException::new);
        String dc = credentials.get(Credentials.DC);
        Optional.ofNullable(dc).orElseThrow(InvalidVendorLineException::new);
        String agent = credentials.get(Credentials.AGENT);
        Optional.ofNullable(agent).orElseThrow(InvalidVendorLineException::new);

        JsonObject params = new JsonObject();
        String encrypted = "";

        try {
            long unixTimestamp = Instant.now().toEpochMilli();
            BigDecimal balance = BigDecimal.ZERO;
            String vendorGameCode = gameSession.getVendorGameCode();
            String[] parts = vendorGameCode.split("_");
            String gType = parts[0];
            String mType = parts[parts.length - 1];

            params.addProperty("action", EndPoints.GAME_URL_ACTION);
            params.addProperty("ts", unixTimestamp);
            params.addProperty("uid", gameSession.getVendorPlayerUsername());
            params.addProperty("parent", agent);
            params.addProperty("balance", balance);
            params.addProperty("lang", gameSession.getVendorLanguageCode());
            params.addProperty("gType", gType);
            params.addProperty("mType", mType);

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
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = formData.getFirst("apiUrl");
        String requestBody = "dc=" + formData.getFirst("dc") + "&x=" + formData.getFirst("encrypted");

        log.info("Calling " + apiUrl);
        log.info("YBNGO GameUrlService: " + formData.getFirst("json").toString());
        log.info("YBNGO Request Body: " + requestBody);

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        GameUrlVo responseVo = null;

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
        RequestLogVo requestLogVo = requestService.createRequestLogVo(apiUrl, requestBody, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        log.info("YBNGO Response Body: " + apiResponse.getBody());

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVo.class);

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
