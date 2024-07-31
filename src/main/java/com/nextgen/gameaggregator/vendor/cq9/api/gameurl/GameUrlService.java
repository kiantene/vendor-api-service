package com.nextgen.gameaggregator.vendor.cq9.api.gameurl;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cq9.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<GameUrlVendorResponseVo> {

    public GameUrlService() {
        super(GameUrlVendorResponseVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.setCredentialApiUrl(com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials.API_URL);
        this.setGameUrl(EndPoints.GAME_URL);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("account", gameSession.getVendorPlayerUsername());
        formData.add("gamehall", "CQ9");
        formData.add("gamecode", gameCode);
        formData.add("gameplat", gameSession.getVendorPlatformCode());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("session", gameSession.getToken());

        return formData;
    }

    @Override
    public HttpHeaders getHeaders(HttpHeaders httpHeaders, MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) {
        String apiToken = credentials.get(Credentials.API_TOKEN);

        if (apiToken == null) {
            log.error(gameSession.getTraceId() + " " + Credentials.API_TOKEN + " is empty");
        } else {
            httpHeaders.add(EndPoints.HEADER_AUTHORIZATION, apiToken);
        }

        return httpHeaders;
    }

    @Override
    public GameUrlVendorResponseVo onResponseSuccess(GameUrlVendorResponseVo responseVo, GameSession gameSession) {
        responseVo.setLeaveUrl(gameSession.getLobbyUrl());

        return responseVo;
    }

//    @Override
//    public GameUrlVendorResponseVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
//            throws InvalidVendorLineException, InvalidVendorResponseException {
//
//        String apiUrl = credentials.get(Credentials.API_URL);
//        Optional.ofNullable(apiUrl).orElseThrow(InvalidVendorLineException::new);
//
//        String apiToken = credentials.get(Credentials.API_TOKEN);
//        Optional.ofNullable(apiToken).orElseThrow(InvalidVendorLineException::new);
//
//        GameUrlVendorResponseVo responseVo = null;
//        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
//        headers.add(EndPoints.HEADER_AUTHORIZATION, apiToken);
//
//        long startTime = System.currentTimeMillis();
//        ResponseEntity<String> apiResponse = WebClient.create(apiUrl)
//                .post()
//                .uri(EndPoints.GAME_URL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .headers(h -> h.addAll(headers))
//                .body(BodyInserters.fromFormData(formData))
//                .retrieve()
//                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
//                .toEntity(String.class)
//                .block();
//
//        long endTime = System.currentTimeMillis();
//        RequestLogVo requestLogVo = requestService.createRequestLogVo(
//                Endpoints.GAME_URL, apiUrl, formData, apiResponse, headers, startTime, endTime,
//                this.getClass().getPackage().getName(), profilesActive);
//
//        try {
//            // 1. validate HTTP Response Code
//            requestService.validateVendorHttpStatusResponse(apiResponse);
//            responseVo = new Gson().fromJson((String) apiResponse.getBody(), GameUrlVendorResponseVo.class);
//            responseVo.setLeaveUrl(gameSession.getLobbyUrl());
//
//            //2. validate vendor response
//            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
//            RequestService.validateResponse(responseVo);
//            RequestService.successResponseLog(requestLogVo);
//        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
//            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
//            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
//            throw new InvalidVendorResponseException(exceptionMsg);
//        }
//
//        return responseVo;
//    }
}
