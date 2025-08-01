package com.nextgen.gameaggregator.game.launcher.inout;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class GameLaunchRequest implements LaunchRequestPayload {
    private String gameMode;
    private UUID aggregatorId;
    private String currency;
    private String authToken;
    private String lang;
    private UUID themeId;
    private String lobbyUrl;
    private String subId;
    private boolean isDemoPlay;
    private String token;
}
