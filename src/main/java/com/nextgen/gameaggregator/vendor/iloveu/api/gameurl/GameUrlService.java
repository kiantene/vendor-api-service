package com.nextgen.gameaggregator.vendor.iloveu.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.iloveu.constant.Credentials;
import com.nextgen.gameaggregator.vendor.iloveu.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.iloveu.constant.Formats;
import com.nextgen.gameaggregator.vendor.iloveu.service.VendorService;
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

    @Autowired
    private HttpService httpService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        String serialNumber = credentials.get(Credentials.SERIAL_NUMBER);
        String apiKey = credentials.get(Credentials.API_SECRET_KEY);
        String token = gameSession.getToken();
        String method = "Login";
        String loginId = gameSession.getVendorPlayerUsername();
        String language = gameSession.getVendorLanguageCode();
        String vendorGameCode = gameSession.getVendorGameCode();
        String platform = gameSession.getVendorPlatformCode();
        String lobbyUrl = gameSession.getLobbyUrl();

        Optional.ofNullable(serialNumber).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);

        //generate encryptString
        String encryptString = token + method + serialNumber + loginId + apiKey;

        //MD5 encrypt
        String md5Param = "";
        try {
            md5Param = vendorService.md5(encryptString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("MD5 Encrypt Failed");
        }

        //setup form data
        formData.add("SN", serialNumber);
        formData.add("ID", token);
        formData.add("Method", method);
        formData.add("LoginId", loginId);
        formData.add("Signature", md5Param);
        formData.add("Language", language);
        formData.add("GameCode", vendorGameCode);
        formData.add("AppType", platform);
        formData.add("CallbackAddress", lobbyUrl);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = credentials.get(Credentials.API_URL);
        String gameUrl = credentials.get(Credentials.GAME_URL);

        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(gameUrl).orElseThrow(InvalidVendorLineException::new);

        // call to Vendor Create API get create player before get game Url
        callCreatePlayer(credentials, gameSession);

        //convert from data into hashmap data
        Map<String, Object> convertFormMap = new LinkedHashMap<>();
        convertFormMap.put("SN", formData.getFirst("SN"));
        convertFormMap.put("ID", formData.getFirst("ID"));
        convertFormMap.put("Method", formData.getFirst("Method"));
        convertFormMap.put("LoginId", formData.getFirst("LoginId"));
        convertFormMap.put("Signature", formData.getFirst("Signature"));
        convertFormMap.put("Language", formData.getFirst("Language"));
        convertFormMap.put("GameCode", formData.getFirst("GameCode"));
        convertFormMap.put("AppType", formData.getFirst("AppType"));
        convertFormMap.put("CallbackAddress", formData.getFirst("CallbackAddress"));

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.LOGIN_URL)
                .build()
                .encode()
                .toUri();

        GameUrlVo responseVo = null;

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, Formats.APPLICATION_JSON);
        headerMap.add(HttpHeaders.ACCEPT, Formats.APPLICATION_JSON);

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .bodyValue(new Gson().toJson(convertFormMap))
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(Formats.RETRY)
                .timeout(Duration.ofMillis(Formats.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.LOGIN_URL, apiUrl, convertFormMap, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            //responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVo.class);
            responseVo = HttpService.convertJsonToDto(apiResponse.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            if(responseVo.getCode().equals("S100")) {
                //S100 = login success
                //construct Login URL
                String loginUrl = responseVo.getDataDto().getLoginUrl();
                loginUrl = loginUrl.replace(EndPoints.REPLACE_DOMAIN, gameUrl) + "&" + gameSession.getVendorGameCode();
                responseVo.setUrl(loginUrl);;

                RequestService.validateResponse(responseVo);
                RequestService.successResponseLog(requestLogVo);
            } else {
                //login fail
                Exception exception = new Exception("Login Fail: " + responseVo.getCode());
                RequestService.failResponseLog(requestLogVo, exception, gameSession);
                throw new InvalidVendorResponseException();
            }

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException | JsonProcessingException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }

        return responseVo;

    }

    public void callCreatePlayer(Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {

        String serialNumber = credentials.get(Credentials.SERIAL_NUMBER);
        String apiKey = credentials.get(Credentials.API_SECRET_KEY);
        String apiUrl = credentials.get(Credentials.API_URL);
        String token = gameSession.getToken();
        String method = "CreatePlayer";
        String playerCode = gameSession.getVendorPlayerUsername();
        String playerName = gameSession.getAgentPlayerUsername();

        Optional.ofNullable(serialNumber).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(apiKey).orElseThrow(InvalidVendorLineException::new);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        //generate encryptString
        String encryptString = token + method + serialNumber + playerCode + apiKey;

        //MD5 encrypt
        String md5Param = "";
        try {
            md5Param = vendorService.md5(encryptString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("MD5 Encrypt Failed");
        }

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("SN", serialNumber);
        formData.put("ID", token);
        formData.put("Method", method);
        formData.put("PlayerCode", playerCode);
        formData.put("PlayerName", playerName);
        formData.put("Signature", md5Param);

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(EndPoints.CREATE_URL)
                .build()
                .encode()
                .toUri();

        CreatePlayerDto createPlayerDto = null;

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add(HttpHeaders.CONTENT_TYPE, Formats.APPLICATION_JSON);
        headerMap.add(HttpHeaders.ACCEPT, Formats.APPLICATION_JSON);

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
                .retry(Formats.RETRY)
                .timeout(Duration.ofMillis(Formats.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.CREATE_URL, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            createPlayerDto = new Gson().fromJson((String) apiResponse.getBody(), CreatePlayerDto.class);

            //2. validate vendor response
            Optional.ofNullable(createPlayerDto).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(createPlayerDto);

            if(createPlayerDto.getCode().equals("S100") || createPlayerDto.getCode().equals("F0005")){
                //S100 = player create success, F0005 = player exist
                RequestService.successResponseLog(requestLogVo);
            } else {
                //create player fail
                Exception exception = new Exception("Create Player Fail: " + createPlayerDto.getCode());
                RequestService.failResponseLog(requestLogVo, exception, gameSession);
                throw new InvalidVendorResponseException();
            }

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }

    }
}
