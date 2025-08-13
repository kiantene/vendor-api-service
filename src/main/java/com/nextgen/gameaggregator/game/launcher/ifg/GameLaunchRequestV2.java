package com.nextgen.gameaggregator.game.launcher.ifg;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequestV2 implements LaunchRequestPayload {
    private String project;
    private String game;
    private String platform;
    private String lang;
    private String demo;
    private String auth;
}
