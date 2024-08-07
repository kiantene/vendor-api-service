package com.nextgen.gameaggregator.vendor.alize.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.alize.constant.Credentials;
import com.nextgen.gameaggregator.vendor.alize.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alize.constant.GameId;
import com.nextgen.gameaggregator.vendor.alize.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    @Autowired
    RequestService requestService;
    @Autowired
    VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
                                                         Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {

        // Get operator and gameUrl by vendor line
        String operator = "";
        String gameUrl = "";
        try {
            operator = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator");
            gameUrl = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "gameUrl");
        } catch (CredentialNotFoundException e) {
            log.error("Credential not found : " + e.getMessage());
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("currency", gameSession.getVendorCurrencyCode());
        formData.add("gameId", this.getGameId(gameSession.getVendorGameCode()));
        formData.add("gamecode", gameSession.getVendorGameCode());
        formData.add("ip", gameSession.getIpAddress());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("operator", operator);
        formData.add("player", gameSession.getVendorPlayerUsername());
        formData.add("playmode", "free");
        formData.add("timestamp", String.valueOf(System.currentTimeMillis()));
        formData.add("url", gameUrl);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
                          GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {

        // Retrieve the API URL and key from the credentials map
        String apiUrl = credentials.get(Credentials.API_URL);
        String apiKey = credentials.get(Credentials.API_KEY);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);

        GameUrlVo responseVo = new GameUrlVo();

        // Generate the signature with the API secret and form data
        String apiSecret = credentials.get(Credentials.SECRET_KEY);
        String signatureBody = this.getSignatureBody(formData);
        Optional.ofNullable(apiSecret).orElseThrow(InvalidVendorLineException::new);
        String signature = VendorService.generateHash(apiSecret, signatureBody);

        // Define headers for the request
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headerMap.add("X-API-Key", apiKey);
        headerMap.add("X-Signature", signature);

        Long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(Endpoints.GAME_URL)
                .headers(header -> header.addAll(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Gson().toJson(formData.toSingleValueMap()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                .block();

        Long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                Endpoints.GAME_URL, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

            // 2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);
            RequestService.successResponseLog(requestLogVo);

            // 3. Regenerate token (Use vendor's game session token)
            String newToken = responseVo.getData().getToken();
            gameSession = gameSessionService.regenerateGameSessionToken(gameSession, newToken);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }

    private String getSignatureBody(MultiValueMap<String, String> formData) {
        // Convert the formData to a JSON object
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
            // Assuming each field only has one value, take the first one
            jsonObject.addProperty(entry.getKey(), entry.getValue().get(0));
        }

        // Convert the JSON object to a string and sort it
        String sortedJson = new Gson().toJson(jsonObject);
        return sortedJson;
    }

    private String getGameId(String vendorGameCode) {
        String gameId = GameId.getGameId(vendorGameCode);
        return gameId;
    }
}
