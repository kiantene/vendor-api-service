package com.nextgen.gameaggregator.core.common;

import com.nextgen.core.exception.Http4xxException;
import com.nextgen.core.exception.Http5xxException;
import com.nextgen.core.webclient.WebClientErrorHandlers;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.util.JsonUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class OperatorApiCallerV2 {
    private final WebClient webClient;

    public OperatorApiCallerV2() {
        this.webClient = createWebClient();
    }

    private WebClient createWebClient() {
        HttpClient httpClient = createHttpClient();
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private HttpClient createHttpClient() {
        return HttpClient.create(createConnectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000) // 2s connect timeout
                .responseTimeout(Duration.ofSeconds(5))             // 5s total read timeout
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(5))); // 5s read timeout (low-level)
    }

    private ConnectionProvider createConnectionProvider() {
        return ConnectionProvider.builder("operator-web-client-pool")
                .maxConnections(1000)                           // Increase total number of simultaneous open connections (default is 500)
                .pendingAcquireMaxCount(1000)                   // Increase the number of queued requests waiting for a connection (default is 500)
                .pendingAcquireTimeout(Duration.ofSeconds(10))  // Reduce wait time for a connection before failing (default is 45s)
                .maxIdleTime(Duration.ofSeconds(30))            // Close idle connections after 30s (default is 0s — no idle timeout)
                .maxLifeTime(Duration.ofMinutes(5))             // Close and recycle connections after 5 minutes to avoid staleness (default is 0s — live forever)
                .evictInBackground(Duration.ofSeconds(60))      // Enable periodic background eviction of idle/stale connections (default is 0s — no eviction cycle)
                .metrics(true)
                .build();
    }

    public ClientBalanceResponse post(String baseUrl, String path, Map<String, String> headers, Object body) {
        String url = buildFullUrl(baseUrl, path);
        LogContext logContext = LogContextHolder.get();
        populateLogStart(logContext, url, body);

        long startNano = System.nanoTime();
        Integer statusCode = null;

        try {
            WebClient.RequestHeadersSpec<?> request = createRequest(url, headers, body);

            ResponseEntity<String> response = executeWithRetry(request, url);

            recordEnd(logContext, startNano, statusCode);

            validateResponse(response, url);

            return parseResponse(response, url);

        } catch (WebClientRequestException ex) {
            /*
            Possible exceptions:
            1. ConnectException - cannot connect to the given host/port
            2. UnknownHostException - host cannot be resolved
            3. SocketTimeoutException/ReadTimeoutException - connection established, but no data received within x seconds
            4. Any other network exception
             */
            throw new OperatorNetworkException(ex.getMessage(), url, ex);

        } catch (Http4xxException ex) {
            statusCode = ex.getStatusCode();
            throw new OperatorApiException(ex.getMessage(), url, ex.getStatusCode(), "", ex);
        } catch (Http5xxException ex) {
            statusCode = ex.getStatusCode();
            throw new OperatorApiException(ex.getMessage(), url, ex.getStatusCode(), "", ex);

        } catch (Exception ex) {
            throw new OperatorApiException("Unexpected client error", ex);

        } finally {
            recordEnd(logContext, startNano, statusCode);
        }
    }

    private ResponseEntity<String> executeWithRetry(WebClient.RequestHeadersSpec<?> request, String path) {
        return request
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, WebClientErrorHandlers::handle4xx)
                .onStatus(HttpStatusCode::is5xxServerError, WebClientErrorHandlers::handle5xx)
                .toEntity(String.class)
                .retryWhen(createRetrySpec(path))
                .block();
    }

    private Retry createRetrySpec(String path) {
        return Retry.backoff(3, Duration.ofSeconds(1))
                .jitter(0.5)
                .filter(this::isRetryable)
                .doBeforeRetry(retrySignal ->
                        log.warn("[{}] Retrying attempt {} due to: {}",
                                path,
                                retrySignal.totalRetries() + 1,
                                retrySignal.failure())
                )
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private WebClient.RequestHeadersSpec<?> createRequest(String url, Map<String, String> headers, Object body) {
        return webClient
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    if (headers != null) headers.forEach(httpHeaders::add);
                })
                .body(BodyInserters.fromValue(body));
    }

    private ClientBalanceResponse parseResponse(ResponseEntity<String> response, String url) {
        LogContext logContext = LogContextHolder.get();
        logContext.setApiResponse(response.getBody());
        return JsonUtils.parseSafely(
                response.getBody(),
                ClientBalanceResponse.class,
                ex -> new OperatorApiException(ex.getMessage(), url,
                        response.getStatusCode().value(), response.getBody(), ex)
        );
    }

    private void populateLogStart(LogContext ctx, String url, Object body) {
        ctx.setApiUrl(url);
        ctx.setApiBody(body);
        ctx.setApiStart(System.currentTimeMillis());
    }

    private void recordEnd(LogContext ctx, long startNano, Integer statusCode) {
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
        ctx.setApiEnd(System.currentTimeMillis());
        ctx.setApiTimeTaken(elapsedMs);
        ctx.setApiStatusCode(statusCode);
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

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof java.io.IOException
                || throwable instanceof io.netty.channel.unix.Errors.NativeIoException
                || (throwable.getCause() instanceof java.io.IOException);
    }
}
