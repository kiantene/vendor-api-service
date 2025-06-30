package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.exception.Http4xxException;
import com.nextgen.gameaggregator.core.exception.Http5xxException;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;

@Component
public class OperatorApiCaller {
    public static final int MAX_IN_MEMORY_SIZE = 5 * 1024 * 1024; // 5MB
    private final WebClient.Builder builder;

    public OperatorApiCaller() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000) // 2s connect timeout
                .responseTimeout(Duration.ofSeconds(3))             // 3s total read timeout
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(3))); // 3s read timeout (low-level)

        this.builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE));
    }

    public <T> T post(String baseUrl, String path, MediaType contentType, Object requestBody, ParameterizedTypeReference<T> typeRef, Map<String, String> headers) {
//        LogContext logContext = LogContextHolder.get();
//        logContext.put("url", baseUrl + path);

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
            requestHeadersSpec = request
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData((MultiValueMap<String, String>) requestBody));
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
            throw new OperatorNetworkException(ex.getMessage(), url);

        } catch (DecodingException ex) {

            throw new OperatorApiException("Invalid response format", ex);

        } catch (Http4xxException e) {
            throw e;
        } catch (Http5xxException e) {
            throw e;
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

    public static String removeTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
