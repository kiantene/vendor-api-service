package com.nextgen.gameaggregator.game.launcher.ezugi;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequest {
    private String language;
    private String token;
    private String operatorId;
    private String homeUrl;
    private String selectGame;
    private String openTable;  
}
