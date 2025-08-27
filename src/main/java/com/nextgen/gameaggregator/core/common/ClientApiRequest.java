package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.core.entity.AgentApiCredential;
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
    private final AgentApiCredential credential;

    public String getBaseUrl() {
        return credential.getCallbackUrl();
    }

    public Map<String, String> getHeaders() {
        String signature = SignatureGenerator.generate(requestObject, credential.getApiSecret());

        return Map.of(
                EndPoints.HEADER_API_KEY, credential.getApiKey(),
                EndPoints.HEADER_SIGNATURE, signature
        );
    }
}
