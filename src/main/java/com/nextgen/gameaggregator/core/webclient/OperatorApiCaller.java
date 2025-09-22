package com.nextgen.gameaggregator.core.webclient;

import com.nextgen.core.webclient.WebClientErrorHandlers;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
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
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class OperatorApiCaller {
    private final WebClient webClient;
    private WebClientRetryPolicy retryPolicy;

    public OperatorApiCaller() {
        this.webClient = createWebClient();
        this.retryPolicy = WebClientRetryPolicy.getDefault();
    }

    public void attachRetryPolicy(WebClientRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    private WebClient createWebClient() {
        HttpClient httpClient = createHttpClient();
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private HttpClient createHttpClient() {
        return HttpClient.create(createConnectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .responseTimeout(Duration.ofSeconds(3))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(3)));
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

    public ClientApiResult post(ClientApiRequest<?> request) {
        return post(request, OperatorCallerLifecycle.noop());
    }

    public ClientApiResult post(ClientApiRequest<?> request, OperatorCallerLifecycle lifecycle) {
        final Instant start = Instant.now();
        ResponseEntity<String> response = null;
        ClientApiResult result = null;
        Exception error = null;
        Instant end = null;

        final String traceId = request.getTraceId();
        final String url = request.getFullUrl();

        try {
            var reqSpec = createRequest(url, request.getHeaders(), request.getRequestObject());
            safe(() -> lifecycle.onBeforeSend(request));

            response = executeWithRetry(reqSpec, url).block();
            end = Instant.now();

            validateResponse(response, url);

            final ClientApiResult success = ClientApiResult.success(traceId, url, response, start, end);
            result = success;

            safe(() -> lifecycle.onResponse(request, success));
            return success;

        } catch (Exception ex) {
            end = Instant.now();
            error = ex;
            result = ClientApiResult.failure(traceId, url, response, start, end, error);

            safe(() -> lifecycle.onError(request, ex));
            return result;

        } finally {
            final Instant finalEnd = (end != null) ? end : Instant.now();
            final ClientApiResult finalResult = (result != null)
                    ? result
                    : new ClientApiResult(traceId, url, response, start, finalEnd, error);

            safe(() -> lifecycle.onComplete(request, finalResult));
        }
    }

    public Mono<ClientApiResult> postAsync(ClientApiRequest<?> request) {
        return postAsync(request, OperatorCallerLifecycle.noop());
    }

    public Mono<ClientApiResult> postAsync(ClientApiRequest<?> request,
                                           OperatorCallerLifecycle lifecycle) {
        final Instant start = Instant.now();
        final String traceId = request.getTraceId();
        final String url = request.getFullUrl();
        final AtomicReference<ResponseEntity<String>> response = new AtomicReference<>();
        final AtomicReference<ClientApiResult> resultRef = new AtomicReference<>();

        return Mono.defer(() -> {
                    // Build request lazily to keep everything deferred until subscribe
                    WebClient.RequestHeadersSpec<?> reqSpec = createRequest(
                            url,
                            request.getHeaders(),
                            request.getRequestObject()
                    );

                    safe(() -> lifecycle.onBeforeSend(request));

                    // Execute without blocking
                    return executeWithRetry(reqSpec, url)
                            .map(resp -> { // Success path
                                final Instant end = Instant.now();
                                validateResponse(resp, url);

                                final ClientApiResult success = ClientApiResult.success(traceId, url, resp, start, end);
                                response.set(resp);
                                resultRef.set(success);

                                safe(() -> lifecycle.onResponse(request, success));
                                return success;
                            })
                            .onErrorResume(ex -> {
                                // Error path, still non-blocking
                                final Instant end = Instant.now();

                                final ClientApiResult failure = new ClientApiResult(
                                        traceId,
                                        url,
                                        null,
                                        start,
                                        end,
                                        (ex instanceof Exception) ? (Exception) ex : new RuntimeException(ex)
                                );
                                resultRef.set(failure);

                                safe(() -> lifecycle.onError(request, ex));
                                return Mono.just(failure);
                            });
                })
                .doFinally(sig -> {
                    ClientApiResult result = resultRef.get();
                    if (result == null) {
                        result = new ClientApiResult(traceId, url, response.get(), start, Instant.now(), null);
                    }
                    final ClientApiResult finalResult = result;

                    safe(() -> lifecycle.onComplete(request, finalResult));
                });
    }

    private Mono<ResponseEntity<String>> executeWithRetry(
            WebClient.RequestHeadersSpec<?> request,
            String path) {

        return request
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, WebClientErrorHandlers::handle4xx)
                .onStatus(HttpStatusCode::is5xxServerError, WebClientErrorHandlers::handle5xx)
                .toEntity(String.class)
                .retryWhen(retryPolicy.retryWhen(path));
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

    private void validateResponse(ResponseEntity<String> response, String url) {
        if (response == null) {
            throw new OperatorApiException("Response is empty", url);
        }
    }

    private void safe(Runnable r) { try { r.run(); } catch (Throwable ignored) {} }
}
