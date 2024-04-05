package com.nextgen.gameaggregator.vendor.winfinity.api.gameurl;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.vendor.winfinity.constant.Credentials;
import com.nextgen.gameaggregator.vendor.winfinity.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.winfinity.service.VendorService;

public class GameUrlService implements GameUrl {

    @Autowired
    private VendorService vendorService;
    @Autowired
    private GameSessionService gameSessionService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession,
            Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode user = objectMapper.createObjectNode();
            user.put("partnerSiteId", credentials.get(Credentials.CLIENT_ID));
            user.put("userId", gameSession.getVendorPlayerUsername());
            user.put("language", gameSession.getVendorLanguageCode());
            user.put("timeZoneOffset", "00:00:00");

            formData.add("user", objectMapper.writeValueAsString(user));
            formData.add("tableId", gameSession.getVendorGameCode());
            formData.add("currency", gameSession.getVendorCurrencyCode());
            formData.add("country", "DE");
            formData.add("device", gameSession.getVendorPlatformCode());
            formData.add("ipAddress", gameSession.getIpAddress());

        } catch (Exception exception) {
            throw new InvalidFormatException(exception.getMessage());
        }

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials,
            GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL) + EndPoints.GAME;
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        String token = vendorService.getToken(gameSession.getVendorLineId());

        GameUrlVo responseVo = null;

        // Set request headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");

        // Set request body
        String requestBody = "{\"user\":{\"partnerSiteId\":\"" + credentials.get(Credentials.CLIENT_ID)
                + "\",\"userId\":\""
                + gameSession.getVendorPlayerUsername() + "\",\"language\":\"" + gameSession.getVendorLanguageCode()
                + "\",\"timeZoneOffset\":\"00:00:00\"},\"tableId\":\"" + gameSession.getVendorGameCode()
                + "\",\"currency\":\"" + gameSession.getVendorCurrencyCode() + "\",\"country\":\"DE\",\"device\":\""
                + gameSession.getVendorPlatformCode() + "\",\"ipAddress\":\"" + gameSession.getIpAddress() + "\"}";

        // Create HTTP entity with headers and body
        HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);

        // Send POST request and retrieve response
        ResponseEntity<String> apiResponse = new RestTemplate().exchange(apiUrl, HttpMethod.POST, httpEntity,
                String.class);

        responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVo.class);

        // Use vendor's msid as session token
        gameSession = gameSessionService.regenerateGameSessionToken(gameSession, responseVo.getData().getMasterSessionId());

        return responseVo;
    }
}
