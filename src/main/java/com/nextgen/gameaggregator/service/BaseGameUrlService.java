package com.nextgen.gameaggregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorResponseException;
import com.nextgen.gameaggregator.operator.game.url.GameUrl;
import com.nextgen.gameaggregator.operator.game.url.GameUrlVo;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseGameUrlService<T extends GameUrlVo> implements GameUrl {
    protected static final Integer TIMEOUT = 8000;
    protected static final Integer RETRY_COUNT = 3;
    protected static final String SELF_GENERATED_GAME_URL = "[Self-generated game url]";
    private final Class<T> responseVoClass;
    private HttpMethod httpMethod;
    private MediaType contentType;
    private String gameUrl;
    private String credentialApiUrl;
    private boolean autoMapResponse;

    protected BaseGameUrlService(Class<T> responseVoClass) {
        this.httpMethod = HttpMethod.POST;
        this.contentType = MediaType.APPLICATION_JSON;
        this.responseVoClass = responseVoClass;
        this.gameUrl = "";
        this.autoMapResponse = true;
    }

    protected void setHttpMethod(HttpMethod method) {
        this.httpMethod = method;
    }

    protected void setContentType(MediaType contentType) {
        this.contentType = contentType;
    }

    protected void setGameUrl(String gameUrl) {
        this.gameUrl = gameUrl;
    }

    protected void setCredentialApiUrl(String credentialApiUrl) {
        this.credentialApiUrl = credentialApiUrl;
    }

    protected void setAutoMapResponse(boolean autoMapResponse) {
        this.autoMapResponse = autoMapResponse;
    }

    @Override
    public GameUrlVo call(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession)
            throws InvalidVendorLineException, InvalidVendorResponseException {

        return null;
    }

    protected HttpHeaders getHeaders(HttpHeaders httpHeaders, MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession) {
        return httpHeaders;
    }

    protected BodyInserter<?, ? super ClientHttpRequest> getBody(MultiValueMap<String, String> formData) {
        if (this.contentType.equals(MediaType.APPLICATION_FORM_URLENCODED)) {
            return BodyInserters.fromFormData(formData);
        }
        return BodyInserters.fromValue(formData.toSingleValueMap());
    }

    protected T onResponseSuccess(T responseVo, GameSession gameSession) {
        return responseVo;
    }

    protected T responseMapper(String responseBody, GameSession gameSession) {
        return null;
    }

    @Override
    public GameUrlVo callToVendor(MultiValueMap<String, String> formData, Map<String, String> credentials, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws InvalidVendorLineException, InvalidVendorResponseException, TimeoutException {

        String apiUrl = credentials.get(this.credentialApiUrl);

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new InvalidVendorLineException(this.credentialApiUrl + " is empty");
        }

        httpRequestLog.setUrl(apiUrl + this.gameUrl);
        AtomicBoolean isTimeout = new AtomicBoolean(false);
        ResponseEntity<String> response;

        HttpHeaders httpHeaders = this.getHeaders(new HttpHeaders(), formData, credentials, gameSession);

        if (this.httpMethod.equals(HttpMethod.POST)) {
            response = this.doPost(apiUrl, this.gameUrl, httpHeaders, formData, isTimeout);
        } else {
            response = this.doGet(apiUrl, this.gameUrl, formData, isTimeout);
        }

        T responseVo = this.validateResponse(response, isTimeout, httpRequestLog, this.responseVoClass, gameSession);

        return this.onResponseSuccess(responseVo, gameSession);
    }

    protected ResponseEntity<String> doPost(String baseUrl, String uri, HttpHeaders httpHeaders, MultiValueMap<String, String> formData, AtomicBoolean isTimeout) {

        WebClient.RequestBodySpec webclient = WebClient.create(baseUrl).post().uri(uri);

        if (!httpHeaders.isEmpty()) {
            webclient.headers(headers -> headers.addAll(httpHeaders));
        }

        return webclient
                .contentType(this.contentType)
                .body(this.getBody(formData))
                .retrieve()
                .toEntity(String.class)
                .retry(RETRY_COUNT)
                .timeout(Duration.ofMillis(TIMEOUT))
                .onErrorResume(TimeoutException.class, e -> {
                    isTimeout.set(true);
                    return Mono.error(e);
                })
                .block();
    }

    private ResponseEntity<String> doGet(String baseUrl, String uri, MultiValueMap<String, String> formData, AtomicBoolean isTimeout) {

        URI getUri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(uri)
                .queryParams(formData)
                .build()
                .encode()
                .toUri();

        return WebClient.create().get().uri(getUri)
                .retrieve()
                .toEntity(String.class)
                .retry(RETRY_COUNT)
                .timeout(Duration.ofMillis(TIMEOUT))
                .onErrorResume(TimeoutException.class, e -> {
                    isTimeout.set(true);
                    return Mono.error(e);
                })
                .block();
    }

    protected T validateResponse(ResponseEntity<String> response, AtomicBoolean isTimeout, HttpRequestLog httpRequestLog, Class<T> responseVoClass, GameSession gameSession)
            throws TimeoutException, InvalidVendorResponseException {

        long endTime = System.currentTimeMillis();
        httpRequestLog.setBetEnd(endTime);

        if (isTimeout.get()) {
            throw new TimeoutException("vendor timeout at " + TIMEOUT + " milliseconds");
        }
        if (response == null) throw new InvalidVendorResponseException("response is null");

        HttpStatusCode httpStatusCode = response.getStatusCode();
        httpRequestLog.setOperatorHttpStatusCode(httpStatusCode.value());
        httpRequestLog.setResponseBody(response.getBody());

        // check is HTTP status code of the request is not 200
        if (httpStatusCode.isError()) {
            throw new InvalidVendorResponseException(String.valueOf(httpStatusCode.value()));
        }

        T responseVo = null;
        if (this.autoMapResponse) {
            try {
                responseVo = new ObjectMapper().readValue(response.getBody(), responseVoClass);

            } catch (JsonProcessingException exception) {
                throw new InvalidVendorResponseException(exception.getMessage());
            }

            if (responseVo.getGameUrl() == null) {
                throw new InvalidVendorResponseException("cannot get game url");
            }
        } else {
            responseVo = this.responseMapper(response.getBody(), gameSession);
        }

        return responseVo;
    }
}
