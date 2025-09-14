package com.nextgen.gameaggregator.core.common;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Getter
@Builder
public class ClientApiRequest<T> {
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final Integer agentId;
    private final HttpMethod method;
    private final String path;
    private final T requestObject;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;

    public Map<String, String> getHeaders() {
        String signature = SignatureGenerator.generate(requestObject, apiSecret);

        return Map.of(
                HEADER_API_KEY, apiKey,
                HEADER_SIGNATURE, signature
        );
    }

    public String getFullUrl() {
        return baseUrl + path;
    }
}
