package com.nextgen.gameaggregator.core.retry;

import com.nextgen.gameaggregator.core.util.JsonUtils;
import com.nextgen.gameaggregator.core.webclient.ClientApiRequest;
import org.springframework.http.HttpMethod;

public final class RetryHelper {
    private static final int TOTAL_PARTITIONS = 8;

    private RetryHelper() {}

    public static HttpCallSpec toHttpCallSpec(ClientApiRequest<?> request) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");

        String json = JsonUtils.toJson(request.getRequestObject());

        return HttpCallSpec.builder()
                .idempotencyKey(request.getTraceId())
                .traceId(request.getTraceId())
                .url(request.getFullUrl())
                .method(request.getMethod() == null ? HttpMethod.POST.name() : request.getMethod().name())
                .headers(request.getHeaders())
                .bodyJson(json)
                .partition(calculatePartition(request))
                .partitionKey(request.getPartitionKey())
                .agentId(request.getAgentId())
                .requestTime(System.currentTimeMillis())
                .transactionTime(request.getTransactionTime())
                .build();
    }

    private static int calculatePartition(Partitionable partitionable) {
        return partitionable.getPartitionKey().hashCode() % RetryHelper.TOTAL_PARTITIONS;
    }
}
