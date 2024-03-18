package com.nextgen.gameaggregator.vendor.bombay.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.bombay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bombay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bombay.constant.Platforms;
import com.nextgen.gameaggregator.vendor.bombay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.Security;
import java.time.Duration;
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
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        // trim game code by removing "_stg" or "_STG"
        String game_code = vendorService.trimGameCode(gameSession.getVendorGameCode());

        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("token", gameSession.getToken());
        formData.add("platform", Platforms.checkPlatformCode(gameSession.getVendorPlatformCode()));
        formData.add("operator_id", credentials.get(Credentials.operator_id));
        formData.add("lobby_url", gameSession.getLobbyUrl());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("game_id", game_code);
        formData.add("currency", gameSession.getVendorCurrencyCode());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        //construct API address
        String urlScheme = credentials.get(Credentials.api_url);

        //check vendor status in our DB
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        // convert multi value map into hash map
        Map<String, Object> hashMap = vendorService.convertToHashMap(formData);

        // convert game code from string into int
        hashMap.put("game_id", Integer.parseInt((String) hashMap.get("game_id")));

        // Convert Map to JSON string using Gson
        Gson gson = new Gson();
        String forDataToString = gson.toJson(hashMap);

        String private_key = credentials.get(Credentials.private_key);

//        String public_key = credentials.get(Credentials.public_key);

        String signature = null;

        // let SHA256 or RSA ignore and bypass the cert(vendor only provide private and public key)
        Security.addProvider(new BouncyCastleProvider());

        try{

            signature = vendorService.generateSignature(forDataToString, private_key);

//            Boolean validateSignature = vendorService.validateSignature(signature, forDataToString, public_key);
//
//            if(!validateSignature){
//                throw new InvalidSignatureException();
//            }
        }catch(Exception e){
            throw new RuntimeException("Error generating signature", e);
        }

        // Assign value for header
        headerMap.add("X-Signature", signature);

        long startTime = System.currentTimeMillis();

        //Construct the API to register player from vendor site
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.LAUNCH_GAME)
                .build()
                .encode()
                .toUri();

        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(forDataToString)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();

        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.LAUNCH_GAME, urlScheme, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        GameUrlVo responseVo = null;

        try{
            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVo.class);

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
