package com.nextgen.gameaggregator.vendor.cg.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.service.VendorGameService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.cg.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.cg.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.DataLengthException;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private VendorGameService vendorGameService;

    @Autowired
    private RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException, DataLengthException, IllegalStateException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("version", String.valueOf(1.0));
        formData.add("language", String.valueOf(gameSession.getVendorLanguageCode()));
        formData.add("channelId", credentials.get(Credentials.AGENT_CHANNEL_ID));
        //encrypt token
        String encodedData = VendorService.encrypt(gameSession.getToken(), credentials.get(Credentials.IV), credentials.get(Credentials.KEY));
        formData.add("data", UriUtils.encode(encodedData, StandardCharsets.UTF_8));

        //for register player only
        formData.add("currency", gameSession.getVendorCurrencyCode());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String urlScheme = credentials.get(Credentials.API_URL);
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        String gameAccessUrl = credentials.get(Credentials.GAME_ACCESS_URL);
        Optional.ofNullable(gameAccessUrl).orElseThrow(InvalidVendorLineException::new);

        String gameCode = gameSession.getVendorGameCode();
        Optional.ofNullable(gameCode).orElseThrow(InvalidVendorLineException::new);

        String agentChannelId = credentials.get(Credentials.AGENT_CHANNEL_ID);
        Optional.ofNullable(agentChannelId).orElseThrow(InvalidVendorLineException::new);

        // get vendor game category
        Map<String, Object> registerPlayer = new HashMap<>();

        registerPlayer.put("accountId", gameSession.getVendorPlayerUsername());
        registerPlayer.put("currency", formData.get("currency").get(0));

        GameUrlVo responseVo = new GameUrlVo();

        //create player data
        Gson gson = new Gson();
        String jsonString = gson.toJson(registerPlayer);
        String playerData;
        playerData = VendorService.encrypt(jsonString, credentials.get(Credentials.IV), credentials.get(Credentials.KEY));

        formData.remove("currency"); //parameter no longer needed

        long startTime = System.currentTimeMillis();

        //construct api url for register
        ResponseEntity<String> apiResponse = createPlayer(urlScheme, playerData, agentChannelId);


        long endTime = System.currentTimeMillis();

        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                gameAccessUrl, urlScheme, formData, apiResponse, null, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            String responseString = apiResponse.getBody();

            //2. validate vendor response
            Optional.ofNullable(responseString).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseString);

            RequestService.successResponseLog(requestLogVo);

            //generate game url
            String gameUrlResponse = generateGameUrl(gameAccessUrl, formData, gameCode);
            responseVo.setGameUrl(gameUrlResponse);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }

    private ResponseEntity<String> createPlayer(String urlScheme, String playerData, String agentChannelId) {
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.REGISTER)
                .build()
                .toUri();

        return WebClient.create()
                .post()
                .uri(uri)
                .bodyValue("version=" + 1.0
                        + "&channelId=" + agentChannelId
                        + "&data=" + playerData)
                .header(HttpHeaders.CONTENT_TYPE, "")
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(3)
                .timeout(Duration.ofMillis(10000))
                .block();
    }

    private String generateGameUrl(String gameAccessUrl, MultiValueMap<String, String> formData, String gameCode) {

        URI uri = UriComponentsBuilder.fromUriString(gameAccessUrl)
                .path(gameCode + "/")
                .queryParams(formData)
                .build(true)//encode escape
                .toUri();

        return uri.toString();
    }

}
