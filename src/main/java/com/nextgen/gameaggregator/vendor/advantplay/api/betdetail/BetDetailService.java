package com.nextgen.gameaggregator.vendor.advantplay.api.betdetail;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.entity.ga.custom.IBetDetailUrlInfo;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.transactions.detail.BetDetailUrl;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.util.RequestLogVo;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.advantplay.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.advantplay.constant.Formats;
import com.nextgen.gameaggregator.vendor.advantplay.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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

@Slf4j
public class BetDetailService implements BetDetailUrl {
    @Autowired
    RequestService requestService;
    @Autowired
    VendorService vendorService;
    @Value("${spring.profiles.active}")
    private String profilesActive;

    @Override
    public MultiValueMap<String, String> formDataBuilder(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorLineException, InvalidFormatException, RecordNotFoundException {

        //convert game code into vendor format
        String gameCode = "";
        if (iBetDetailUrlInfo.getGameCode().startsWith("AP_")) {
            gameCode = iBetDetailUrlInfo.getGameCode().substring("AP_".length());
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Token", null); // this token will set after callPlayerToken() vendor will return a token
        formData.add("BrandCode", Formats.BRAND_CODE);
        formData.add("SiteCode", Formats.SITE_CODE);
        formData.add("GameCode", gameCode);
        formData.add("GameRoundId", iBetDetailUrlInfo.getExternalRoundId());
        formData.add("LangCode", vendorLanguageCode.getLanguageCode());

        return formData;
    }

    @Override
    public BetDetailUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo, VendorLanguageCode vendorLanguageCode)
            throws InvalidVendorResponseException, InvalidVendorLineException {

        String gameApiUrl = credentials.get(Credentials.WEB_URL);
        Optional.ofNullable(gameApiUrl).orElseThrow(InvalidVendorLineException::new);

        String prefix = VendorService.validateCredential(credentials.get(Credentials.PREFIX));

        // call to Vendor Authorize API get Token to build login game Url
        PlayerTokenDto playerTokenDto = callPlayerToken(credentials, iBetDetailUrlInfo);

        // Build Bet Detail Url
        formData.set("Token", playerTokenDto.getToken());

        String gameUrl = UriComponentsBuilder.fromUriString(gameApiUrl)
                .path(prefix)
                .path(EndPoints.GAME_DETAIL)
                .queryParams(formData)
                .build()
                .encode()
                .toUri()
                .toString();

        BetDetailUrlVo betDetailUrlVo = new BetDetailUrlVo(gameUrl);

        if (betDetailUrlVo.getBetDetailUrl() == null) {
            throw new InvalidVendorResponseException();
        }

        return betDetailUrlVo;
    }

    public PlayerTokenDto callPlayerToken(Map<String, String> credentials, IBetDetailUrlInfo iBetDetailUrlInfo) throws InvalidVendorLineException, InvalidVendorResponseException {

        String apiUrl = VendorService.validateCredential(credentials.get(Credentials.API_URL));
        String prefix = VendorService.validateCredential(credentials.get(Credentials.PREFIX));

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("Timestamp", VendorService.getTimestamp());
        formData.put("Seq", iBetDetailUrlInfo.getBetId());
        formData.put("BrandCode", Formats.BRAND_CODE);
        formData.put("SiteCode", Formats.SITE_CODE);
        formData.put("PlayerId", iBetDetailUrlInfo.getVendorUsername());

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .path(prefix)
                .path(EndPoints.GET_PLAYER_TOKEN)
                .build()
                .encode()
                .toUri();

        PlayerTokenDto playerTokenDto = null;

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<String, String>();

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> apiResponse = WebClient.create()
                .post()
                .uri(uri)
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
                EndPoints.GET_PLAYER_TOKEN, apiUrl, formData, apiResponse, headerMap, startTime, endTime,
                this.getClass().getPackage().getName(), profilesActive);

        GameSession gameSession = new GameSession();

        try {

            // 1. validate HTTP Response Code
            assert apiResponse != null;
            requestService.validateVendorHttpStatusResponse(apiResponse);
            playerTokenDto = new Gson().fromJson(apiResponse.getBody(), PlayerTokenDto.class);

            //2. validate vendor response
            Optional.ofNullable(playerTokenDto).orElseThrow(InvalidVendorResponseException::new);
            RequestService.validateResponse(playerTokenDto);

            RequestService.successResponseLog(requestLogVo);

        } catch (HttpResponseStatusCodeException | JsonSyntaxException | InvalidResponseException invalidException) {
            RequestService.failResponseLog(requestLogVo, invalidException, gameSession);
            throw new InvalidVendorResponseException();
        }

        return playerTokenDto;
    }
}
