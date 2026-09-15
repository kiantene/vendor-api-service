package com.nextgen.gameaggregator.game.launcher.esoterica;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GameLaunchRequest {
    private String secretLogin;
    private String gameName;
    private String externalPlayerId;
    private String playMode;
    private String token;
    private String hash;
}
