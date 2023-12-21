package com.nextgen.gameaggregator.vendor.pgsoft.api.gameurl;

import com.nextgen.gameaggregator.service.S3Service;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pgsoft.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Autowired
    S3Service s3Service;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        boolean checkExists = this.checkIsGameOpenInNewFormat(credentials.get(Credentials.GAME_VERSION), gameCode);

        if (checkExists) {
            formData = this.newDataFormat(gameCode, gameSession, credentials);

        } else {
            formData = this.oldDataFormat(gameCode, gameSession);

        }

        return formData;

    }

    private boolean checkIsGameOpenInNewFormat(String gameVersionOrGameList, String gameCode) {
        boolean exists = false;

        if (gameVersionOrGameList.equalsIgnoreCase("new")) {
            exists = true;

        } else {
            String[] elements = StringUtils.tokenizeToStringArray(gameVersionOrGameList.trim(), ",");

            for (String element : elements) {
                if (element.equals(gameCode)) {
                    exists = true;
                    break;
                }
            }

        }

        return exists;

    }

    private MultiValueMap<String, String> oldDataFormat(String gameCode, GameSession gameSession) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("gameCode", gameCode);
        formData.add("language", gameSession.getLanguage());
        formData.add("token", gameSession.getToken());

        return formData;

    }

    private MultiValueMap<String, String> newDataFormat(String gameCode, GameSession gameSession, Map<String, String> credentials) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String path = "/" + gameCode + "/index.html";
        String extraArgs = "btt=1&ops=" + gameSession.getToken();

        formData.add("trace_id", gameSession.getTraceId());
        formData.add("path", path);
        formData.add("operator_token", credentials.get(Credentials.OPERATOR_TOKEN));
        formData.add("extra_args", extraArgs);
        formData.add("url_type", "game-entry");
        formData.add("client_ip", gameSession.getIpAddress());

        return formData;

    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        GameUrlVo responseVo = new GameUrlVo();

        boolean checkExists = this.checkIsGameOpenInNewFormat(credentials.get(Credentials.GAME_VERSION), gameSession.getVendorGameCode());

        if (checkExists) {
            responseVo = this.newCallFormat(formData, credentials, gameSession);

        } else {
            responseVo = this.oldCallFormat(formData, credentials, gameSession);

        }

        return responseVo;
    }

    private GameUrlVo oldCallFormat(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) {
        GameUrlVo responseVo = new GameUrlVo();

        // 4. Get Vendor URL Scheme by vendorPlayer's vendor_line_id
        String urlScheme = credentials.get(Credentials.URL_SCHEME);
        // 5. Lookup Game Code
        String gameCode = formData.get("gameCode").get(0);
        String language = formData.get("language").get(0);
        // 6. Generate game session token to embed into urlScheme and save into game session table
        String token = formData.get("token").get(0);
        // 7. Retrieve Operator Token as it is required to form the Game URL
        String operatorToken = credentials.get(Credentials.OPERATOR_TOKEN);
        // 8. Retrieve Operator lobby URL
        String lobbyUrl = gameSession.getLobbyUrl();
        // 9. Construct the Game URL
        String gameUrl = VendorService.generateGameUrl(urlScheme, gameCode, language, operatorToken, token, lobbyUrl);
        // 10. Save this player's game session
        // Set the game URL and return to Operator
        responseVo.setGameUrl(gameUrl);

        return responseVo;
    }

    private GameUrlVo newCallFormat(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {

        RequestLogVo requestLogVo = null;
        ResponseEntity<String> apiResponse = null;
        GameUrlVo responseVo = new GameUrlVo();

        try {
            String apiUrl = credentials.get(Credentials.GAME_URL_DOMAIN);
            Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

            String rawText = this.encodeFormData(formData);
            MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();

            Long startTime = System.currentTimeMillis();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            WebClient webClient = WebClient.create();
            apiResponse = webClient
                    .post()
                    .uri(apiUrl)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(BodyInserters.fromValue(rawText))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                    .toEntity(String.class)
                    .retry(3)
                    .timeout(Duration.ofMillis(Endpoints.TIMEOUT))
                    .block();

            Long endTime = System.currentTimeMillis();
            requestLogVo = requestService.createRequestLogVo(
                    apiUrl, apiUrl, rawText, apiResponse, headerMap, startTime, endTime,
                    this.getClass().getPackage().getName(), profilesActive);

            requestService.validateVendorHttpStatusResponse(apiResponse);
            String vendorGameUrl = s3Service.GenerateHtmlToS3(gameSession.getToken(), apiResponse.getBody());
            responseVo.setGameUrl(vendorGameUrl);

            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);
            return responseVo;

        } catch (InvalidResponseException | HttpResponseStatusCodeException e){
            RequestService.failResponseLog(requestLogVo, e, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);

        } catch (Exception e){
            throw new InvalidVendorLineException(e.getMessage());

        }

    }

    private String encodeFormData(MultiValueMap<String, String> formData) throws Exception {
        StringBuilder encodedData = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (encodedData.length() > 0) {
                    encodedData.append("&");
                }
                encodedData.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
                encodedData.append("=");
                encodedData.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
            }
        }

        return encodedData.toString();
    }

}
