package com.nextgen.gameaggregator.game.launcher.lucky365;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {

    private String sn;
    private String id;
    private String method;
    private String loginId;
    private String signature;
    private String language;
    private String gameCode;
    private Integer appType;
    private String callbackAddress;
    private Integer deviceType;

}
