package com.nextgen.gameaggregator.core.webclient;

import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.retry.Partitionable;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Getter
public class ClientApiRequest<T> implements Partitionable {
    private static final Gson GSON = new Gson();
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final String traceId;
    private final Integer agentId;
    private final String agentPlayerUsername;
    private final HttpMethod method;
    private final String baseUrl;
    private final String path;
    private final T requestObject;
    private final String apiKey;
    private final String apiSecret;
    private final String signature;
    private final Long transactionTime;

    @Builder(builderClassName = "ClientApiRequestBuilder", buildMethodName = "doBuild")
    private ClientApiRequest(String traceId,
                             Integer agentId,
                             String agentPlayerUsername,
                             HttpMethod method,
                             String baseUrl,
                             String path,
                             T requestObject,
                             String apiKey,
                             String apiSecret,
                             String signature,
                             Long transactionTime) {
        this.traceId = traceId;
        this.agentId = agentId;
        this.agentPlayerUsername = agentPlayerUsername;
        this.method = method;
        this.baseUrl = baseUrl;
        this.path = path;
        this.requestObject = requestObject;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.transactionTime = transactionTime;

        // Auto-generate signature if not provided
        if (signature == null && requestObject != null && apiSecret != null) {
            this.signature = generateSignature(requestObject, apiSecret);
        } else {
            this.signature = signature;
        }
    }

    @Override
    public int calculatePartition(int totalPartitions) {
        return this.agentId.hashCode() % totalPartitions;
    }

    @Override
    public String getPartitionKey() {
        return this.agentPlayerUsername;
    }

    @Override
    public Long getTransactionTime() {
        return this.transactionTime;
    }

    public static class ClientApiRequestBuilder<T> {
        public ClientApiRequest<T> build() {
            return doBuild();
        }
    }

    public Map<String, String> getHeaders() {
        return Map.of(
                HEADER_API_KEY, apiKey,
                HEADER_SIGNATURE, signature
        );
    }

    public String getFullUrl() {
        return removeTrailingSlash(baseUrl) + path;
    }

    private String removeTrailingSlash(String url) {
        return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }

    private String generateSignature(Object payload, String secret) {
        String json = GSON.toJson(payload);
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(json);
    }
}
