package com.nextgen.gameaggregator.core.retry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.webclient.ClientApiRequest;
import org.springframework.http.HttpMethod;

public final class RetryHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    private RetryHelper() {}

    public static HttpCallSpec toHttpCallSpec(ClientApiRequest<?> request) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");

        String json = null;
        if (request.getRequestObject() != null) {
            try {
                json = mapper.writeValueAsString(request.getRequestObject());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize request object", e);
            }
        }

        return HttpCallSpec.builder()
                .idempotencyKey(request.getSignature())
                .traceId(request.getTraceId())
                .url(request.getFullUrl())
                .method(request.getMethod() == null ? HttpMethod.POST.name() : request.getMethod().name())
                .headers(request.getHeaders())
                .bodyJson(json)
                .requestTime(System.currentTimeMillis())
                .build();
    }
}
