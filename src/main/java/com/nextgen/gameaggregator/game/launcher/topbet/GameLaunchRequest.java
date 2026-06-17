package com.nextgen.gameaggregator.game.launcher.topbet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequest {
    private String pid;
    private String ver;
    private String method;
    private String username;
    private Integer app_id;
    private String ip;
    private String lang;
    private String sign;
}
