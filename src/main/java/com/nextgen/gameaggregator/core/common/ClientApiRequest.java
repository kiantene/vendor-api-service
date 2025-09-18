package com.nextgen.gameaggregator.core.common;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Getter
public class ClientApiRequest<T> {
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final String traceId;
    private final Integer agentId;
    private final HttpMethod method;
    private final String path;
    private final T requestObject;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;
    private final String signature;

    @Builder(builderClassName = "ClientApiRequestBuilder", buildMethodName = "doBuild")
    private ClientApiRequest(String traceId,
                             Integer agentId,
                             HttpMethod method,
                             String path,
                             T requestObject,
                             String baseUrl,
                             String apiKey,
                             String apiSecret,
                             String signature) {
        this.traceId = traceId;
        this.agentId = agentId;
        this.method = method;
        this.path = path;
        this.requestObject = requestObject;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        // Auto-generate signature if not provided
        if (signature == null && requestObject != null && apiSecret != null) {
            this.signature = SignatureGenerator.generate(requestObject, apiSecret);
        } else {
            this.signature = signature;
        }
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
        return baseUrl + path;
    }
}
