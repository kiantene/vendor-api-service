package com.nextgen.gameaggregator.game.launcher.endorphina;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {
    private String exit;
    private String lang;
    private String nodeId;
    private String token;
    private String sign;
}
