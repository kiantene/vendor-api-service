package com.nextgen.gameaggregator.game.launcher.mtlive;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequest {

    @JsonProperty("system_code")
    private String systemCode;

    @JsonProperty("web_id")
    private String webId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("language")
    private String language;
}