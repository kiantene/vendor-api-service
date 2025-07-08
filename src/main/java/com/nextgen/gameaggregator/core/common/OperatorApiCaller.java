package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.exception.Http4xxException;
import com.nextgen.gameaggregator.core.exception.Http5xxException;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.exception.OperatorNetworkException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
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
    private final WebClient.Builder builder;
    private String path;

    @Autowired
    public OperatorApiCaller() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000) // 2s connect timeout
                .responseTimeout(Duration.ofSeconds(3))             // 3s total read timeout
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(3))); // 3s read timeout (low-level)

        this.builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    public OperatorApiCaller(String path) {
        this();
        this.path = path;
    }

    public <T> ClientBalanceResponse post(ClientRequestAuth<T> requestAuth) {
        return post(
                requestAuth.getCallback(),
                requestAuth.getHeaders(),
                requestAuth.getRequestObject()
        );
    }

    public ClientBalanceResponse post(String baseUrl, Map<String, String> headers, Object requestBody) {
        return post(baseUrl, this.path, headers, requestBody);
    }

    public ClientBalanceResponse post(String baseUrl, String path, Map<String, String> headers, Object requestBody) {
//        LogContext logContext = LogContextHolder.get();
//        logContext.put("operatorUrl", baseUrl + path);

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

        try {
//            long startTime = System.currentTimeMillis();
//            logContext.put("operatorStart", startTime);

            ClientBalanceResponse clientBalanceResponse = requestHeadersSpec
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                    .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
                    .bodyToMono(ClientBalanceResponse.class)
                    .block();
//            long endTime = System.currentTimeMillis();
//            long timeTaken = endTime - startTime;
//            logContext.put("operatorEnd", endTime);
//            logContext.put("operatorTimeTaken", timeTaken);

            return clientBalanceResponse;

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
}
