package com.nextgen.gameaggregator.game.launcher.vplus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequest {
    private String appId;
    private String timestamp;
    private String sign;
    private String back;
    private String token;
    private String id;
    private String lang;
    private String closeBack;
}
