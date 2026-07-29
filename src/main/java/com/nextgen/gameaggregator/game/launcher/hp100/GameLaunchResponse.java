package com.nextgen.gameaggregator.game.launcher.hp100;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameLaunchResponse {
    private String gameUrl;
}
