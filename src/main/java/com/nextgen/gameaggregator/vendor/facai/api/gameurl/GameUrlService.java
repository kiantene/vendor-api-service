package com.nextgen.gameaggregator.vendor.facai.api.gameurl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.GameType;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
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

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        VendorService vendorService = new VendorService();

        //map request param and convert to json string
        Map<String, Object> loginParam = new HashMap<String, Object>();
        loginParam.put("MemberAccount", gameSession.getVendorPlayerUsername());
        loginParam.put("GameID", gameSession.getVendorGameCode());
        loginParam.put("LanguageID", gameSession.getVendorLanguageCode());
        loginParam.put("JackpotStatus", GameType.ENABLE_JACKPOT);
        String jsonParamString = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonParamString = objectMapper.writeValueAsString(loginParam);
            log.info("RAW Param Json : " + jsonParamString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("Json Convert Failed");
        }

        //encrypt request param
        String encryptParam = "";
        try {
            encryptParam = vendorService.aesEncrypt(jsonParamString, credentials.get(Credentials.AGENT_KEY));
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("Param Encrypt Failed");
        }

        //MD5 encrypt
        String md5Param = "";
        try {
            md5Param = VendorService.md5(jsonParamString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("MD5 Encrypt Failed");
        }

        //setup form data
        formData.add("AgentCode", credentials.get(Credentials.AGENT_CODE));
        formData.add("Currency", gameSession.getVendorCurrencyCode());
        formData.add("Params", encryptParam);
        formData.add("Sign", md5Param);

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {
        String apiUrl = credentials.get(Credentials.API_URL);
        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);

        log.info("Calling " + apiUrl + EndPoints.GAME_URL);
        log.info(formData.toString());

        //convert from data into mapper data
        Map<String, String> convertFormMap = new HashMap<String, String>();
        convertFormMap.put("AgentCode", formData.getFirst("AgentCode"));
        convertFormMap.put("Currency", formData.getFirst("Currency"));
        convertFormMap.put("Params", formData.getFirst("Params"));
        convertFormMap.put("Sign", formData.getFirst("Sign"));

        //convert mapper data into json string
        String jsonFormString = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonFormString = objectMapper.writeValueAsString(convertFormMap);
            log.info("Request Json : " + jsonFormString);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidVendorLineException("Json Convert Failed");
        }

        GameUrlVo responseVo = null;
        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        //post request to vendor API with JSON string
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
                .post()
                .uri(EndPoints.GAME_URL)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(BodyInserters.fromObject(jsonFormString))
                .retrieve()
                .toEntity(String.class)
                .block();

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                EndPoints.GAME_URL, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo = HttpService.convertJsonToDto(String.valueOf(apiResponse.getBody()), GameUrlVo.class);

            //2. validate vendor response
            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException |
                 JsonProcessingException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException);
            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(exceptionMsg);
        }

        return responseVo;

    }
}
