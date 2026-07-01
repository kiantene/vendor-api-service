package com.nextgen.gameaggregator.game.launcher.mtlive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameLaunchDataResponse {
    private String token;

    private String url;
}
