package com.nextgen.gameaggregator.game.launcher.winfinity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetBearerTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
}
