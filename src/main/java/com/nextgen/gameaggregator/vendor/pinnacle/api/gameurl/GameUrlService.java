package com.nextgen.gameaggregator.vendor.pinnacle.api.gameurl;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.PinnacleVendorPlayer;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.repository.PinnacleVendorUsernameRepository;
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

    @Autowired
    private PinnacleVendorUsernameRepository pinnacleVendorUsernameRepository;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) 
        throws InvalidVendorLineException, InvalidFormatException {
        String token = getToken();
        String userCode = getUserCode(gameSession, token);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("userCode", userCode);
        formData.add("locale", "zh-cn");
        formData.add("oddsFormat", "HK");

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
        throws InvalidVendorLineException, InvalidVendorResponseException {
        String token = getToken();

        String apiUrl = "https://paapistg.oreo88.com/b2b/player/login";
        GameUrlVo responseVo = null;
        HttpHeaders headerMap = createHeaders("PX142", token);

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(apiUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headerMap))
                .bodyValue(formData)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.isError(), response -> Mono.empty())
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

            // 2. validate vendor response
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }

    private String getToken() {
        try {
            return vendorService.generateToken("PX142", "a1068064-d32e-4b0a-971c-d3ea502a08c3", "tR5yueCxHALL2P7v");
        } catch (Exception exception) {
            log.error("Error generating token", exception);
            return null;
        }
    }

    private String getUserCode(GameSession gameSession, String token) {
        String userCode = "";
        Optional<PinnacleVendorPlayer> pinnacleVendorPlayer = pinnacleVendorUsernameRepository.findByUsername(gameSession.getVendorPlayerUsername());

        if (pinnacleVendorPlayer.isPresent()) {
            userCode = pinnacleVendorPlayer.get().getVendorPlayerUsername();
        } else {
            userCode = createUserCode(gameSession, token);
        }

        return userCode;
    }

    private String createUserCode(GameSession gameSession, String token) {
        String userCode = "";
        String apiCreateUrl = "https://paapistg.oreo88.com/b2b/player/create";
        HttpHeaders headerMap = createHeaders("PX142", token);

        ResponseEntity<String> apiCreateResponse = WebClient.create()
                .post()
                .uri(apiCreateUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headerMap))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.isError(), response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        try {
            JsonParser jsonParser = JsonParserFactory.getJsonParser();
            userCode = jsonParser.parseMap(apiCreateResponse.getBody()).get("userCode").toString();
        } catch (Exception ex) {
            log.error(apiCreateUrl, ex);
        }

        // Temporary store vendor player username into couchbase
        PinnacleVendorPlayer entity = new PinnacleVendorPlayer();
        entity.setId(gameSession.getVendorPlayerUsername() + "_" + userCode);
        entity.setUsername(gameSession.getVendorPlayerUsername());
        entity.setVendorPlayerUsername(userCode);
        create(entity);

        return userCode;
    }

    private HttpHeaders createHeaders(String userCode, String token) {
        HttpHeaders headerMap = new HttpHeaders();
        headerMap.add("userCode", userCode);
        headerMap.add("token", token);
        return headerMap;
    }

    public PinnacleVendorPlayer create(PinnacleVendorPlayer entity) {
        return pinnacleVendorUsernameRepository.save(entity);
    }
}
