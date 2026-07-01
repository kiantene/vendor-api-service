package com.nextgen.gameaggregator.game.launcher.mtlive;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GameLaunchResponse {

    private String code;

    private String message;

    private String timestamp;

    private GameLaunchDataResponse data;
}
