package com.nextgen.gameaggregator.vendor.db.api.gameurl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.db.constant.Credentials;
import com.nextgen.gameaggregator.vendor.db.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.db.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {

    @Autowired
    RequestService requestService;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        return new LinkedMultiValueMap<>();
    }

    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        //get and verify API address
        String urlScheme = credentials.get(Credentials.API_URL);
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);
        Map<String, String> formDataJson = new HashMap<>();
        formDataJson.put("memberId", gameSession.getVendorPlayerUsername());
        formDataJson.put("memberName", gameSession.getVendorPlayerUsername());
        formDataJson.put("memberPwd", gameSession.getVendorPlayerUsername());
        formDataJson.put("deviceType", gameSession.getVendorPlatformCode());
        formDataJson.put("memberIp", gameSession.getIpAddress());
        formDataJson.put("gameId", gameSession.getVendorGameCode());

        //generate Url with queryString
        long unixTimestamp = System.currentTimeMillis() / 1000L;
        MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
        param.add("agent", credentials.get(Credentials.AGENT));
        param.add("timestamp", String.valueOf(unixTimestamp));
        String randomNumber = String.valueOf(VendorService.generateRandom10DigitNumber());
        param.add("randno", randomNumber);

        //create md5 sign
        String concat = VendorService.removeSpacesAndBrackets(
                param.get("agent") + Objects.requireNonNull(param.get("timestamp")).toString()
                        + credentials.get(Credentials.SECRET_KEY));
        String md5 = VendorService.md5Generator(concat);
        String insertRandomStrmd5 = VendorService.insertRandomString(md5);
        param.add("sign", insertRandomStrmd5);

        //multivaluemap to queryString
        String queryString = VendorService.convertToQueryString(param);

        //create json body
        String encryptBody = new Gson().toJson(formDataJson);
        encryptBody = VendorService.encryptAES(encryptBody,
                credentials.get(Credentials.SECRET_KEY), credentials.get(Credentials.IV));
        GameUrlVo responseVo;

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();
        headerMap.add("Content-Type", MediaType.TEXT_PLAIN);
        long startTime = System.currentTimeMillis();
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.LOGIN_URL)
                .query(queryString)
                .build()
                .encode()
                .toUri();
        assert encryptBody != null;
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
                .headers(requestService.setHeaders(headerMap))
                .bodyValue(encryptBody)
                .retrieve()
                // TODO: to catch more error codes
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.LOGIN_URL + "?" + queryString, urlScheme, encryptBody, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {
            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = new Gson().fromJson(apiResponse.getBody(), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (NullPointerException | HttpResponseStatusCodeException | JsonSyntaxException |
                 InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            String exceptionMsg = apiResponse.toString();
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;
    }

}
