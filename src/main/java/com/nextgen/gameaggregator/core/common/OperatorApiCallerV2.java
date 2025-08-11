package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.core.exception.Http4xxException;
import com.nextgen.core.exception.Http5xxException;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.util.JsonUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;

public class OperatorApiCallerV2 {

    private final WebClient.Builder builder;
    private String path;

    public OperatorApiCallerV2() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(3))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(3)));

        this.builder = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    public OperatorApiCallerV2(String path) {
        this();
        this.path = path;
    }

    public <T> ClientBalanceResponse post(ClientRequestAuth<T> requestAuth) {
        return post(
                requestAuth.getBaseUrl(),
                requestAuth.getPath(),
                requestAuth.getHeaders(),
                requestAuth.getRequestObject()
        );
    }

    public ClientBalanceResponse post(String baseUrl, Map<String, String> headers, Object body) {
        return post(baseUrl, this.path, headers, body);
    }

    public ClientBalanceResponse post(String baseUrl, String path, Map<String, String> headers, Object body) {
        String url = buildFullUrl(baseUrl, path);
        LogContext logContext = LogContextHolder.get();
        populateLogStart(logContext, url, body);

        long startNano = System.nanoTime();

        try {
            WebClient.RequestHeadersSpec<?> request = createRequest(url, headers, body);

            ResponseEntity<String> response = request
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                    .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
                    .toEntity(String.class)
                    .block();

            recordEnd(logContext, startNano);
            validateResponse(response, url);

            return JsonUtils.parseSafely(
                    response.getBody(),
                    ClientBalanceResponse.class,
                    ex -> new OperatorApiException(ex.getMessage(), url, response.getStatusCode().value(), response.getBody(), ex)
            );

        } catch (WebClientRequestException ex) {
            /*
            Possible exceptions:
            1. ConnectException - cannot connect to the given host/port
            2. UnknownHostException - host cannot be resolved
            3. SocketTimeoutException/ReadTimeoutException - connection established, but no data received within x seconds
            4. Any other network exception
             */
            throw new OperatorNetworkException(ex.getMessage(), url, ex);

        } catch (Http4xxException | Http5xxException ex) {
            /*
            Client connection succeed, but returned 4xx or 5xx status code
             */
            throw new OperatorApiException(ex.getMessage(), ex);

        } catch (Exception ex) {
            throw new OperatorApiException("Unexpected client error", ex);

        } finally {
            recordEnd(logContext, startNano);
        }
    }

    private WebClient.RequestHeadersSpec<?> createRequest(String url, Map<String, String> headers, Object body) {
        return builder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    if (headers != null) headers.forEach(httpHeaders::add);
                })
                .body(BodyInserters.fromValue(body));
    }

    private void populateLogStart(LogContext ctx, String url, Object body) {
        ctx.setApiUrl(url);
        ctx.setApiBody(body);
        ctx.setApiStart(System.currentTimeMillis());
    }

    private void recordEnd(LogContext ctx, long startNano) {
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
        ctx.setApiEnd(System.currentTimeMillis());
        ctx.setApiTimeTaken(elapsedMs);
    }

    private String buildFullUrl(String baseUrl, String path) {
        if (baseUrl == null) return path;
        return removeTrailingSlash(baseUrl) + (path != null ? path : "");
    }

    private String removeTrailingSlash(String url) {
        return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }

    private void validateResponse(ResponseEntity<String> response, String url) {
        if (response == null) {
            throw new OperatorApiException("Response is empty", url);
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
}
