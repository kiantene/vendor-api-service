package com.nextgen.gameaggregator.vendor.dreamgaming.api.gameurl;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidFormatException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.service.BaseGameUrlService;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.dreamgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.dreamgaming.service.VendorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
@Getter
public class GameUrlService extends BaseGameUrlService<DGGameUrlVo> {

    @Value("${spring.profiles.active}")
    private String profilesActive;

    public GameUrlService() {
        super(DGGameUrlVo.class);
        this.setAutoMapResponse(false);
    }

    @Override
    public MultiValueMap<String, String> formDataBuilder(String gameCode, GameSession gameSession, Map<String, String> credentials) throws InvalidVendorLineException, InvalidFormatException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("username", gameSession.getVendorPlayerUsername());
        formData.add("password", VendorService.md5Generator(gameSession.getVendorPlayerUsername()));
        formData.add("currencyName", gameSession.getCurrencyCode());
        formData.add("winLimit", credentials.get(Credentials.WIN_LIMIT));

        return formData;
    }

    @Override
    public DGGameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog) throws InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {
        //construct API address
        String launchUrl = Optional.of(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);

        // Trigger create member function by calling vendor api
        try {
            ResponseEntity<String> apiResponse = this.checkAndCreateAccount(formData, credentials);
            if (apiResponse != null) {
                httpRequestLog.setResponseBody(apiResponse.getBody());
            }
        } catch (Exception e) {
            throw new InvalidVendorResponseException(e.getMessage());
        }

        AtomicBoolean isTimeout = new AtomicBoolean(false);
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Create MultiValueMap for headers
        MultiValueMap<String, String> headers = generateHeaders(credentials, timestamp);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.addAll(headers);

        URI uri = UriComponentsBuilder.fromUriString(launchUrl)
                .path(EndPoints.LOGIN)
                .build()
                .encode()
                .toUri();

        // Trigger doPost to get game url function by calling vendor api
        ResponseEntity<String> apiResponse2 = this.doPost(launchUrl, uri.toString(), httpHeaders, formData, isTimeout);

        this.validateResponse(apiResponse2, isTimeout, httpRequestLog, DGGameUrlVo.class, gameSession);

        DGGameUrlVo responseVo = new Gson().fromJson(apiResponse2.getBody(), DGGameUrlVo.class);

        httpRequestLog.setUrl(responseVo.getGameUrl());

        return responseVo;
    }

    private ResponseEntity<String> checkAndCreateAccount(MultiValueMap<String, String> formData, Map<String, String> credentials)
            throws InvalidVendorLineException {
        //construct API address & check vendor status in our DB
        String urlScheme = Optional.of(credentials.get(Credentials.API_URL))
                .orElseThrow(InvalidVendorLineException::new);

        URI uri = UriComponentsBuilder.fromUriString(urlScheme)
                .path(EndPoints.SIGNUP)
                .build()
                .encode()
                .toUri();

        String timestamp = String.valueOf(System.currentTimeMillis());
        // Create MultiValueMap for headers
        MultiValueMap<String, String> headers = generateHeaders(credentials, timestamp);

        return WebClient.create()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(this.getBody(formData))
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.empty())
                .toEntity(String.class)
                .retry(EndPoints.RETRY)
                .timeout(Duration.ofMillis(EndPoints.TIMEOUT))
                .block();
    }

    private MultiValueMap<String, String> generateHeaders(Map<String, String> credentials, String timestamp) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("agent", credentials.get(Credentials.AGENT_ID));
        headers.add("sign", VendorService.signGenerator(credentials, timestamp));
        headers.add("time", timestamp);
        return headers;
    }

}
