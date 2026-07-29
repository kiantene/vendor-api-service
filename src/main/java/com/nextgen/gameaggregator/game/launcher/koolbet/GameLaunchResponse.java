package com.nextgen.gameaggregator.game.launcher.koolbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameLaunchResponse {
    @JsonProperty("Data")
    private String data;
    @JsonProperty("DisplayMode")
    private int displayMode;
    @JsonProperty("ErrorCode")
    private int errorCode;
    @JsonProperty("Message")
    private String message;
}
