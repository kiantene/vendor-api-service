package com.nextgen.gameaggregator.vendor.pinnacle.api.gameurl;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.HttpResponseStatusCodeException;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidResponseException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorService vendorService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws 
        InvalidVendorLineException, InvalidFormatException {
            String token = null;

            try {
                token = vendorService.generateToken("PX142", "a1068064-d32e-4b0a-971c-d3ea502a08c3", "tR5yueCxHALL2P7v");
            }  catch (Exception exception) {
                log.error(token, exception);
            }
        
            String apiCreateUrl = "https://paapistg.oreo88.com/b2b/player/create";
            MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
            headerMap.add("userCode", "PX142");
            headerMap.add("token", token);

            ResponseEntity<String> apiCreateResponse = WebClient.create()
                .post()
                .uri(apiCreateUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headerMap))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

            String userCode = "";

            try {
                JsonParser jsonParser = JsonParserFactory.getJsonParser();
                userCode = jsonParser.parseMap(apiCreateResponse.getBody()).get("userCode").toString();

            } catch (Exception ex) {
                log.error(apiCreateUrl, ex);
            }

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            try {
                formData.add("userCode", userCode);
                formData.add("locale", "zh-cn");
                formData.add("oddsFormat", "HK");

            }  catch (Exception exception) {
                throw new InvalidFormatException(exception.getMessage());
            }

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
        GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
            String token = null;

            try {
                token = vendorService.generateToken("PX142", "a1068064-d32e-4b0a-971c-d3ea502a08c3", "tR5yueCxHALL2P7v");
            }  catch (Exception exception) {
                log.error(token, exception);
            }
        
            String apiUrl = "https://paapistg.oreo88.com/b2b/player/login";
            GameUrlVo responseVo = null;
            MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
            headerMap.add("userCode", "PX142");
            headerMap.add("token", token);

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
                responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

                //2. validate vendor response
                Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
                RequestService.validateResponse(responseVo);
                RequestService.successResponseLog(requestLogVo);

            } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
                RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
                String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
                throw new InvalidVendorResponseException(exceptionMsg);
            }

        return responseVo;
    }
}
