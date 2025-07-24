package com.nextgen.gameaggregator.game.launcher.aviatorstudio;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest implements LaunchRequestPayload {
    private String token;
    private String providerId;
    private String currency;
    private String language;
    private String gameId;
}
