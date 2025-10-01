package com.nextgen.gameaggregator.game.launcher.winfinity.bearer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class BearerTokenRequest {
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("client_secret")
    private String clientSecret;
    @JsonProperty("grant_type")
    private String grantType;
    private String[] scope;
}
