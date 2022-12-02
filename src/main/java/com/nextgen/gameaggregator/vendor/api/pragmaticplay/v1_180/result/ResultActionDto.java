package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.result;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultActionDto {
    private String hash;
    private String userId;
    private String gameId;
    private String roundId;
    private String amount;
    private String reference;
    private String providerId;
    private String timestamp;
    private String roundDetails;
    private String token;
}
