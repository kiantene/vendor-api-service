package com.nextgen.gameaggregator.vendor.queenmaker.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Credentials;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Formats;
import com.nextgen.gameaggregator.vendor.queenmaker.constant.Platforms;
import com.nextgen.gameaggregator.vendor.queenmaker.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;
    @Autowired
    VendorService vendorService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        String vendorGameCode = gameSession.getVendorGameCode();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        // Split the gameCode into two parts based on the underscore character "_"
        String[] parts = VendorService.splitGameCode(vendorGameCode, 2);
        String gpCode = parts[0];
        String gCode = parts[1];

        // if not direct lobby then set param
        if(!gCode.equalsIgnoreCase("Lobby")) {
            formData.add("gpcode", gpCode);
            formData.add("gcode", gCode);
        }
        
        formData.add("token", null); // this token will set after callAuthorize() vendor will return a token
        formData.add("lang", gameSession.getVendorLanguageCode());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String gameApiUrl = credentials.get(Credentials.GAME_API_URL);
        Optional.ofNullable(gameApiUrl).orElseThrow(InvalidVendorLineException::new);

        // call to Vendor Authorize API get Token to build login game Url
        AuthorizeDto authorizeDto = callAuthorize(credentials, gameSession);

        // Build Game Url
        formData.set("token", authorizeDto.getAuthtoken());

        // set game path for direct lobby (default direct game)
        String gCode = VendorService.splitGameCode(gameSession.getVendorGameCode(), 2)[1];
        String gamePath = EndPoints.GAME_URL;
        if(gCode.equalsIgnoreCase("Lobby")) {
            if(gameSession.getVendorPlatformCode().equals(Platforms.WEB.toString())){
                // set lobby path for desktop mode
                gamePath = credentials.get(Credentials.LOBBY_PATH_DESKTOP);
            }else{
                // set lobby path for mobile mode
                gamePath = credentials.get(Credentials.LOBBY_PATH_MOBILE);
            }
            Optional.ofNullable(gamePath).orElseThrow(InvalidVendorLineException::new);
        }

        String gameUrl = UriComponentsBuilder.fromUriString(gameApiUrl)
                .path(gamePath)
                .queryParams(formData)
                .build()
                .encode()
                .toUri()
                .toString();

        GameUrlVo gameUrlVo = new GameUrlVo(gameUrl);

        if (gameUrlVo.getGameUrl() == null) {
            throw new InvalidVendorResponseException();
        }

        return gameUrlVo;
    }

    public AuthorizeDto callAuthorize(Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {

        String clientId = credentials.get(Credentials.CLIENT_ID);
        String clientSecret = credentials.get(Credentials.CLIENT_SECRET);
        String apiUrl = credentials.get(Credentials.API_URL);
        String ipAddress = gameSession.getIpAddress();

        Optional.ofNullable(clientId).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(clientSecret).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(ipAddress).orElseThrow(InvalidVendorLineException::new);

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("ipaddress", ipAddress);
        formData.put("username", gameSession.getVendorPlayerUsername());
        formData.put("userid", gameSession.getVendorPlayerUsername());
        formData.put("lang", gameSession.getVendorLanguageCode());
        formData.put("cur", gameSession.getVendorCurrencyCode());
        formData.put("betlimitid", Formats.BRONZE);
        formData.put("istestplayer", Formats.REAL_PLAYER);
        formData.put("platformtype", Integer.parseInt(gameSession.getVendorPlatformCode()));

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.AUTHORIZE)
                .build()
                .encode()
                .toUri();

        AuthorizeDto authorizeDto = null;

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, Formats.APPLICATION_JSON);
        headerMap.add(HttpHeaders.ACCEPT, Formats.APPLICATION_JSON);
        headerMap.add(Formats.HEADER_CLIENT_ID, clientId);
        headerMap.add(Formats.HEADER_CLIENT_SECRET, clientSecret);

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .bodyValue(new Gson().toJson(formData))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.AUTHORIZE, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            authorizeDto = new Gson().fromJson((String) apiResponse.getBody(), AuthorizeDto.class);

            //2. validate vendor response
            Optional.ofNullable(authorizeDto).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(authorizeDto);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }

        return authorizeDto;
    }
}
