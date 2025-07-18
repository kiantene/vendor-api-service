package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.exception.Http4xxException;
import com.nextgen.gameaggregator.core.exception.Http5xxException;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class OperatorApiCaller {
    public static final String AGENT_ID = "agentId";
    public static final String ACTION = "action";
    private final WebClient.Builder builder;
    private final MeterRegistry meterRegistry;

    public OperatorApiCaller(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        ConnectionProvider provider = ConnectionProvider.builder("high-volume-pool")
                .maxConnections(1000)                           // Increase total number of simultaneous open connections (default is 500)
                .pendingAcquireMaxCount(1000)                   // Increase the number of queued requests waiting for a connection (default is 500)
                .pendingAcquireTimeout(Duration.ofSeconds(10))  // Reduce wait time for a connection before failing (default is 45s)
                .maxIdleTime(Duration.ofSeconds(30))            // Close idle connections after 30s (default is 0s — no idle timeout)
                .maxLifeTime(Duration.ofMinutes(5))             // Close and recycle connections after 5 minutes to avoid staleness (default is 0s — live forever)
                .evictInBackground(Duration.ofSeconds(60))      // Enable periodic background eviction of idle/stale connections (default is 0s — no eviction cycle)
                .metrics(true)
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000) // 2s connect timeout
                .responseTimeout(Duration.ofSeconds(3))             // 3s total read timeout
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(3))); // 3s read timeout (low-level)

        this.builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    public ResponseEntity<String> post(String baseUrl, String path, Map<String, String> headers, Object requestBody, Map<String, String> extra) {
        String agentId = extra.getOrDefault(AGENT_ID, "unknown");
        String action = extra.getOrDefault(ACTION, "unspecified");

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

        WebClient.RequestHeadersSpec<?> requestHeadersSpec = request
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody));

        Instant operatorStart = Instant.now();
        try {
            return requestHeadersSpec
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                    .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
                    .toEntity(String.class)
                    .retryWhen(
                            Retry.backoff(3, Duration.ofSeconds(1)) // Retry up to 3 times with exponential backoff: 1s, 2s, 4s delays
                                    .jitter(0.5) // Add ±50% random jitter to avoid retry spikes under load (e.g., jittered 1s = 0.5s–1.5s)
                                    .filter(this::isRetryable) // Only retry for retryable exceptions (e.g., I/O errors, 5xx responses); skip 4xx errors
                                    .doBeforeRetry(retrySignal ->
                                            log.warn("[{}] Retrying attempt {} due to: {}",
                                                    path,
                                                    retrySignal.totalRetries() + 1,
                                                    retrySignal.failure())
                                    )
                                    .onRetryExhaustedThrow((spec, signal) -> signal.failure()) // Re-throw the last failure after all retries are exhausted
                    )
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
            throw new OperatorNetworkException(ex.getMessage(), url, ex);

        } catch (DecodingException ex) {
            /*
            Thrown by ".bodyToMono(ClientBalanceResponse.class)"
            If client responded a format that cannot be decoded to a proper json object
             */

            throw new OperatorApiException("Invalid response format", ex);

        } catch (Http4xxException | Http5xxException ex) {
            /*
            Client connection succeed, but returned 4xx or 5xx status code
             */
            throw new OperatorApiException(ex.getMessage(), ex);

        } catch (Exception e) {

            throw new OperatorApiException("Unexpected client error", e);
        } finally {
            recordOperatorLatency(agentId, action, operatorStart);
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

    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof java.io.IOException
                || throwable instanceof io.netty.channel.unix.Errors.NativeIoException
                || (throwable.getCause() instanceof java.io.IOException);
    }

    private void recordOperatorLatency(String agentId, String action, Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        Timer.builder("operator.latency")
                .description("Latency of operator API call")
                .tag("agentId", agentId)
                .tag("action", action)
                .register(meterRegistry)
                .record(duration);
    }
}
