package com.nextgen.gameaggregator.vendor.advantplay.api.gameurl;

import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.advantplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.advantplay.service.VendorService;
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
    RequestService requestService;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials)
            throws InvalidVendorLineException, InvalidFormatException {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Token", gameSession.getToken());
        formData.add("BrandCode", Formats.BRAND_CODE);
        formData.add("SiteCode", Formats.SITE_CODE);
        formData.add("GameCode", gameSession.getVendorGameCode());
        formData.add("LangCode", gameSession.getVendorLanguageCode());
        Optional.ofNullable(gameSession.getLobbyUrl()).filter(value -> !value.isEmpty()).ifPresent(value -> formData.add("BackUrl", value));

        return formData;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = VendorService.validateCredential(credentials.get(Credentials.API_URL));
        String webUrl = VendorService.validateCredential(credentials.get(Credentials.WEB_URL));
        String prefix = VendorService.validateCredential(credentials.get(Credentials.PREFIX));

        GameUrlVo responseVo = new GameUrlVo();

        URI uri = UriComponentsBuilder.fromUriString(webUrl)
                .path(prefix)
                .path(EndPoints.LAUNCH_GAME)
                .queryParams(formData)
                .build()
                .encode()
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
            responseVo.setUrl(uri.toString());

            //2. validate vendor response
//            Optional.ofNullable(responseVo).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(responseVo);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException |
                 JsonSyntaxException |
                 InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
//            String exceptionMsg = apiResponse != null ? apiResponse.toString() : "";
            throw new InvalidVendorResponseException(apiResponse.toString());
        }

        return responseVo;
    }
}
