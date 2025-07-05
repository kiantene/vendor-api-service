package com.nextgen.gameaggregator.core.common;

import com.nextgen.gameaggregator.operator.constant.EndPoints;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ClientRequestAuth {
    private final Integer agentId;
    private final Object requestObject;

    public ClientRequestAuth(Integer agentId, Object requestObject) {
        this.agentId = agentId;
        this.requestObject = requestObject;
    }

    public Map<String, String> getHeaders() {
        String apiKey = "";
        String signature = SignatureGenerator.generate(this.requestObject, "");

        return Map.ofEntries(
                Map.entry(EndPoints.HEADER_API_KEY, apiKey),
                Map.entry(EndPoints.HEADER_SIGNATURE, signature)
        );
    }

    public String getCallback() {
        return "";
    }
}
