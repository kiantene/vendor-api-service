package com.nextgen.gameaggregator.vendor.pinnacle.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import lombok.extern.slf4j.Slf4j;
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Autowired
    private RequestService requestService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorPlayerService vendorPlayerService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        String oddsFormat = Optional.ofNullable(credentials.get(Credentials.ODDS_FORMAT)).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("userCode", gameSession.getVendorPlayerUsername());
        formData.add("locale", gameSession.getVendorLanguageCode());
        formData.add("oddsFormat", oddsFormat);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = Optional.ofNullable(credentials.get(Credentials.LOGIN_URL)).orElseThrow(InvalidVendorLineException::new);
        String agentCode = Optional.ofNullable(credentials.get(Credentials.AGENT_CODE)).orElseThrow(InvalidVendorLineException::new);
        String agentKey = Optional.ofNullable(credentials.get(Credentials.AGENT_KEY)).orElseThrow(InvalidVendorLineException::new);
        String secretKey = Optional.ofNullable(credentials.get(Credentials.SECRET_KEY)).orElseThrow(InvalidVendorLineException::new);

        String userCode = formData.toSingleValueMap().getOrDefault("userCode", null);
        Boolean validVendorPlayerUsername = VendorService.isCorrectVendorPlayerUsername(userCode, agentCode);
        if (Objects.equals(validVendorPlayerUsername, Boolean.FALSE)) {
            createUserCode(gameSession, credentials);
            formData.put("userCode", List.of(gameSession.getVendorPlayerUsername()));
        }

        GameUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add("userCode", agentCode);
        headerMap.add("token", vendorService.generateToken(agentCode, agentKey, secretKey));

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(apiUrl)
                .headers(header -> header.addAll(headerMap))
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
            requestService.validateVendorHttpStatusResponse(Objects.requireNonNull(apiResponse));
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

    private void createUserCode(GameSession gameSession, Map<String, String> credentials) throws InvalidVendorResponseException, InvalidVendorLineException {

        String apiCreateUrl = Optional.ofNullable(credentials.get(Credentials.CREATE_URL)).orElseThrow(InvalidVendorLineException::new);
        String agentCode = Optional.ofNullable(credentials.get(Credentials.AGENT_CODE)).orElseThrow(InvalidVendorLineException::new);
        String agentKey = Optional.ofNullable(credentials.get(Credentials.AGENT_KEY)).orElseThrow(InvalidVendorLineException::new);
        String secretKey = Optional.ofNullable(credentials.get(Credentials.SECRET_KEY)).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add("userCode", agentCode);
        headerMap.add("token", vendorService.generateToken(agentCode, agentKey, secretKey));

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiCreateResponse = WebClient.create()
                .post()
                .uri(apiCreateUrl)
                .headers(headers -> headers.addAll(headerMap))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                "", apiCreateUrl, new LinkedMultiValueMap<>(), apiCreateResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(Objects.requireNonNull(apiCreateResponse));

            JsonParser jsonParser = JsonParserFactory.getJsonParser();
            String userCode = jsonParser.parseMap(apiCreateResponse.getBody()).get("userCode").toString();

            vendorPlayerService.updateNewVendorPlayerUsername(gameSession, userCode);

            RequestService.successResponseLog(requestLogVo);
        } catch (Exception ex) {
            RequestService.failResponseLog(requestLogVo, ex, gameSession);
            String exceptionMsg = apiCreateResponse != null ? apiCreateResponse.toString() : "";

            throw new InvalidVendorResponseException(exceptionMsg);
        }
    }
}
