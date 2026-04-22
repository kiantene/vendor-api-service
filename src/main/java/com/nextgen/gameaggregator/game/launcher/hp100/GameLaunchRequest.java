package com.nextgen.gameaggregator.game.launcher.hp100;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GameLaunchRequest {
    private String secretKey;
    private String sessionId;
    private Boolean isDemo;
    private Boolean isMobile;
    private String gameId;
    private String partnerId;
}
