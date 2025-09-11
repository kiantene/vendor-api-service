package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.operator.constant.EndPoints;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ClientApiRequest<T> {
    private final Integer agentId;
    private final String path;
    private final T requestObject;
    private final String baseUrl;
    private final String apiKey;
    private final String apiSecret;

    public Map<String, String> getHeaders() {
        String signature = SignatureGenerator.generate(requestObject, getApiSecret());

        return Map.of(
                EndPoints.HEADER_API_KEY, getApiKey(),
                EndPoints.HEADER_SIGNATURE, signature
        );
    }
}
