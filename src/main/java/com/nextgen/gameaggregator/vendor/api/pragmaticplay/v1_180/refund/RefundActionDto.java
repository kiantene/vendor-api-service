package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.refund;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundActionDto {
    private String hash;
    private String userId;
    private String reference;
    private String providerId;
    private String token;
}
