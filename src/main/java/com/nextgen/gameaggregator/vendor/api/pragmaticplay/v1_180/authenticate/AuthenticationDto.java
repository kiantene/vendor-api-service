package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.authenticate;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationDto {
    private String hash;
    private String token;
    private String providerId;
}
