package com.nextgen.gameaggregator.game.launcher.egtdigital;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {
    private String sessionToken;
    private String casinoId;
    private String playerId;
    private String gameKey;
    private String currencyCode;
    private String closeUrl;
    private String demo;
}
