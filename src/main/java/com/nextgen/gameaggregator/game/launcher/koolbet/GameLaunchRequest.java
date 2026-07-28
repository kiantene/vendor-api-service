package com.nextgen.gameaggregator.game.launcher.koolbet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequest {
    @JsonProperty("Token")
    private String token;

    @JsonProperty("GameId")
    private String gameId;

    @JsonProperty("Lang")
    private String lang;

    @JsonProperty("HomeUrl")
    private String homeUrl;

    @JsonProperty("Platform")
    private String platform;

    @JsonProperty("disableFullScreen")
    private Integer disableFullScreen;

    @JsonProperty("AgentId")
    private String agentId;

    @JsonProperty("Key")
    private String key;
}
