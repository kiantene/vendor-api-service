package com.nextgen.gameaggregator.game.launcher.inout;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class GameLaunchRequest {
    private UUID aggregatorId;
    private String subId;
    private String gameMode;
    private String currency;
    private String authToken;
    private String lang;
    private boolean adaptive;
    private boolean isDemoPlay;
    private String token;
    private String lobbyUrl;
}
