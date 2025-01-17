package com.nextgen.gameaggregator.vendor.gpkpushgaming.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.service.RequestService;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.constant.Platforms;
import com.nextgen.gameaggregator.vendor.gpkpushgaming.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class GameUrlService extends BaseGameUrlService<PGGameUrlVo> {
    @Autowired
    RequestService requestService;

    String apiToken = "api_token";
    String platform = "platform";
    String timestamp = "timestamp";

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public GameUrlService() {
        super(PGGameUrlVo.class);
        this.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        // trim game code by removing "_stg" or "_STG"
        String vendorGameCode = VendorService.trimGameCode(gameSession.getVendorGameCode());

        formData.add(apiToken, credentials.get(Credentials.API_TOKEN));
        formData.add("user", gameSession.getVendorPlayerUsername());
        formData.add("password", gameSession.getVendorPlayerUsername());
        formData.add(platform, credentials.get(Credentials.PLATFORM_ID));
        formData.add(timestamp, String.valueOf(VendorService.getCurrentTime()));
        formData.add("mode", vendorGameCode);
        formData.add("home_url", gameSession.getLobbyUrl());
        formData.add("lang", gameSession.getVendorLanguageCode());
        formData.add("client_type", Platforms.checkPlatformCode(gameSession.getVendorPlatformCode()));
        formData.add("ip", gameSession.getIpAddress());
        formData.add("country", credentials.get(Credentials.COUNTRY));
        formData.add("city", credentials.get(Credentials.CITY));
        formData.add("homeUrl", gameSession.getLobbyUrl());

        return formData;
    }

    @Override
    public PGGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {
        //construct API address
        String launchUrl = credentials.get(Credentials.API_URL);

        // Trigger create member function by calling vendor api
        try {
            ResponseEntity<String> apiResponse = this.checkAndCreateAccount(formData, credentials, gameSession);
            if (apiResponse != null) {
                httpRequestLog.setResponseBody(apiResponse.getBody());
            }
        } catch (Exception e) {
            throw new InvalidVendorResponseException(e.getMessage());
        }

        AtomicBoolean isTimeout = new AtomicBoolean(false);

        HttpHeaders httpHeaders = this.getHeaders(new HttpHeaders(), formData, credentials, gameSession);

        URI uri = UriComponentsBuilder.fromUriString(launchUrl)
                .path(EndPoints.LAUNCH_GAME)
                .build()
                .encode()
                .toUri();

        // Trigger doPost to get game url function by calling vendor api
        ResponseEntity<String> apiResponse2 = this.doPost(launchUrl, uri.toString(), httpHeaders, formData, isTimeout);

        this.validateResponse(apiResponse2, isTimeout, httpRequestLog, PGGameUrlVo.class, gameSession);

        PGGameUrlVo responseVo = new Gson().fromJson(apiResponse2.getBody(), PGGameUrlVo.class);

        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }

    private ResponseEntity<String> checkAndCreateAccount(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException {
        //construct API address & check vendor status in our DB
        String urlScheme = Optional.of(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);

        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.CREATE_PLAYER)
                .build()
                .encode()
                .toUri();


        MultiValueMap<String, String> createPlayer = new LinkedMultiValueMap<>();

        createPlayer.put(apiToken, formData.get(apiToken));
        createPlayer.put("user", formData.get("user"));
        createPlayer.put("password", formData.get("user"));
        createPlayer.put("username", formData.get("user"));
        createPlayer.add("currency", gameSession.getVendorCurrencyCode());
        createPlayer.put(platform, formData.get(platform));
        createPlayer.put(timestamp, formData.get(timestamp));

        return WebClient.create()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(createPlayer)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();
    }
}
