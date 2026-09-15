package com.nextgen.gameaggregator.game.launcher.aviatrix;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"cid", "productId", "sessionToken", "isDemo", "lang", "lobbyUrl"})
public class GameLaunchRequest {
    private String cid;
    private String productId;
    private String sessionToken;
    @JsonProperty("isDemo")
    private boolean demo;
    private String lang;
    private String lobbyUrl;
}
