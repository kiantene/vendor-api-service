package com.nextgen.gameaggregator.core.webclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.Http4xxException;
import com.nextgen.core.exception.Http5xxException;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import com.nextgen.gameaggregator.core.webclient.exception.ClientApiResponseParseException;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Instant;
import java.util.Optional;

@Getter
public class ClientApiResult {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String traceId;
    private final String url;
    private final ResponseEntity<String> response;
    private final Instant start;
    private final Instant end;
    private final String rawResponse;
    private final HttpStatusCode statusCode;
    private final Exception error;

    public static ClientApiResult success(String traceId,
                                          String url,
                                          ResponseEntity<String> response,
                                          Instant start,
                                          Instant end) {

        return new ClientApiResult(traceId, url, response, start, end, null);
    }

    public static ClientApiResult failure(String traceId,
                                          String url,
                                          ResponseEntity<String> response,
                                          Instant start,
                                          Instant end,
                                          Exception error) {

        return new ClientApiResult(traceId, url, response, start, end, error);
    }

    public ClientApiResult(String traceId,
                           String url,
                           ResponseEntity<String> response,
                           Instant start,
                           Instant end,
                           Exception error) {

        this.traceId = traceId;
        this.url = url;
        this.response = response;
        this.start = start;
        this.end = end;
        this.error = error;

        if (response != null) {
            this.rawResponse = response.getBody();
            this.statusCode = response.getStatusCode();
        } else {
            this.rawResponse = null;
            this.statusCode = null;
        }
    }

    public <T> T parseTo(Class<T> type) {
        try {
            return objectMapper.readValue(rawResponse, type);
        } catch (JsonProcessingException e) {
            throw new ClientApiResponseParseException(
                    "Failed to parse API response into " + type.getSimpleName(),
                    rawResponse,
                    e
            );
        }
    }

    public void throwIfError() {
        if (error == null) return;

        String resp = Optional.ofNullable(this.rawResponse).orElse("");

        if (error instanceof WebClientRequestException wre) {
            throw new OperatorNetworkException(wre.getMessage(), this.url, wre);
        }
        if (error instanceof Http4xxException ex4xx) {
            throw new OperatorApiException(ex4xx.getMessage(), this.url, ex4xx.getStatusCode(), resp, ex4xx);
        }
        if (error instanceof Http5xxException ex5xx) {
            throw new OperatorApiException(ex5xx.getMessage(), this.url, ex5xx.getStatusCode(), resp, ex5xx);
        }
        if (error instanceof RuntimeException re) {
            throw re;
        }
        int code = statusCode != null ? statusCode.value() : -1;
        throw new OperatorApiException("Unexpected client error", this.url, code, resp, error);
    }
}
