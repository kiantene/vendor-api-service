package com.nextgen.gameaggregator.game.launcher.wazdan;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {

    private String operator;
    private String game;
    private String token;
    private String license;
    private String lang;
    private String platform;
    private String lobbyUrl;
    private String mode;
}
