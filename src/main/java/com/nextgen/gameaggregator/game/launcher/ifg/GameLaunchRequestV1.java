package com.nextgen.gameaggregator.game.launcher.ifg;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequestV1 implements LaunchRequestPayload {
    private String partner;
    private String gameName;
    private String platform;
    private String lang;
    private String demo;
    private String key;
}
