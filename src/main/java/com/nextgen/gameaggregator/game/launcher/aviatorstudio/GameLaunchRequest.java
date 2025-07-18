package com.nextgen.gameaggregator.game.launcher.aviatorstudio;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameLaunchRequest {
    private String token;
    private String providerId;
    private String currency;
    private String language;
    private String gameId;
    private String backtoHome;
    private String fullscreen;
}
