package com.nextgen.gameaggregator.core.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

@Component
public class WebClientApiCaller {
    private final WebClient.Builder builder;
    private String path;
    private MediaType contentType;

    public WebClientApiCaller() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000) // 3s connect timeout
                .responseTimeout(Duration.ofSeconds(5))             // 5s total read timeout
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(5))); // 5s read timeout (low-level)

        this.builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    public WebClientApiCaller(String path, MediaType contentType) {
        this();
        this.path = path;
        this.contentType = contentType;
    }

    public <T> T post(String baseUrl, Map<String, String> headers, Object requestBody, ParameterizedTypeReference<T> typeRef) {
        return post(baseUrl, path, contentType, headers, requestBody, typeRef);
    }

    public <T> T post(String baseUrl, String path, MediaType contentType, Map<String, String> headers, Object requestBody, ParameterizedTypeReference<T> typeRef) {
        LogContext logContext = LogContextHolder.get();
        logContext.put("url", baseUrl + path);

        WebClient.RequestBodySpec request = builder
                .baseUrl(baseUrl)
                .build()
                .post()
                .uri(path);

        String url = removeTrailingSlash(baseUrl) + path;

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request = request.header(entry.getKey(), entry.getValue());
            }
        }

        WebClient.RequestHeadersSpec<?> requestHeadersSpec;

        if (contentType.equals(MediaType.APPLICATION_FORM_URLENCODED)) {
            MultiValueMap<String, String> formData = this.convertToMultiValueMap(requestBody);
            requestHeadersSpec = request
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData));
        } else {
            requestHeadersSpec = request
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody));
        }

        try {
            return requestHeadersSpec
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                    .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
                    .bodyToMono(typeRef)
                    .block()
                    ;

        } catch (WebClientRequestException ex) {
            /*
            Possible exceptions:
            1. ConnectException - cannot connect to the given host/port
            2. UnknownHostException - host cannot be resolved
            3. SocketTimeoutException/ReadTimeoutException - connection established, but no data received within x seconds
            4. Any other network exception
             */
            throw new VendorNetworkException(ex.getMessage(), url, ex);

        } catch (DecodingException ex) {
            /*
            Thrown by ".bodyToMono(ClientBalanceResponse.class)"
            If client responded a format that cannot be decoded to a proper json object
             */

            throw new VendorApiException("Invalid response format", ex);

        } catch (Http4xxException | Http5xxException ex) {
            /*
            Client connection succeed, but returned 4xx or 5xx status code
             */
            throw new VendorApiException(ex.getMessage(), ex);

        } catch (Exception e) {

            throw new RuntimeException("Unexpected client error", e);
        }
    }

    private Mono<? extends Throwable> handle4xx(ClientResponse response) {
        return response.bodyToMono(String.class)
                .map(body -> new Http4xxException(response.statusCode().value(), body));
    }

    private Mono<? extends Throwable> handle5xx(ClientResponse response) {
        return response.bodyToMono(String.class)
                .map(body -> new Http5xxException(response.statusCode().value(), body));
    }

    private String removeTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private MultiValueMap<String, String> convertToMultiValueMap(Object dto) {
        final ObjectMapper objectMapper = new ObjectMapper();
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        Map<String, Object> map = objectMapper.convertValue(dto, new TypeReference<>() {});

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Collection<?> collection) {
                collection.forEach(item -> multiValueMap.add(entry.getKey(), String.valueOf(item)));
            } else if (value != null) {
                multiValueMap.add(entry.getKey(), String.valueOf(value));
            }
        }

        return multiValueMap;
    }
}
