package com.nextgen.gameaggregator.vendor.aviatrix.api.gameurl;

import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.Credentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class GameUrlService implements GameUrl {
    
    @Autowired
    private RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("cid", credentials.get(Credentials.CID)); //cid from vendor
        formData.add("sessionToken", gameSession.getToken());
        formData.add("productId", gameSession.getVendorGameCode()); //game code
        formData.add("lang", gameSession.getVendorLanguageCode());

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) throws InvalidVendorLineException, InvalidVendorResponseException {
        String urlScheme = credentials.get(Credentials.LAUNCH_URL);
        Optional.ofNullable(urlScheme).orElseThrow(InvalidVendorLineException::new);

        GameUrlVo responseVo = new GameUrlVo();

        //build uri
        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path("/")
                .queryParams(formData)
                .build()
                .toUri();

        long startTime = System.currentTimeMillis();

        ResponseEntity<String> apiResponse = ResponseEntity.ok().body(uri.toString());

        long endTime = System.currentTimeMillis();
        RequestLogVo requestLogVo = requestService.createRequestLogVo(
                uri.getPath(), uri.getScheme() + "://" + uri.getHost(), formData, apiResponse, new LinkedMultiValueMap<String, String>(), startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        try {

            // 1. validate HTTP Response Code
            requestService.validateVendorHttpStatusResponse(apiResponse);
            responseVo.setGameUrl(uri.toString());

            //2. validate vendor response
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException(apiResponse.toString());
        }

        return responseVo;
    }

}
