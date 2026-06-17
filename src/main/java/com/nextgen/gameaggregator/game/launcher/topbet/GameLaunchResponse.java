package com.nextgen.gameaggregator.game.launcher.topbet;

import lombok.Data;

@Data
public class GameLaunchResponse {
    private Integer code;
    private String message;
    private String username;
    private String url;
}
