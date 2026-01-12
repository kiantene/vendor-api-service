package com.nextgen.gameaggregator.game.launcher.spribe;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameLaunchRequest {
    private String user;
    private String token;
    private String lang;
    private String currency;
    private String operator;
}
