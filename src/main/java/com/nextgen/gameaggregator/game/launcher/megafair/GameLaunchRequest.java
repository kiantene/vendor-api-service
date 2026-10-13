package com.nextgen.gameaggregator.game.launcher.megafair;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {

    private String token;
    private String bankId;
    private String gameId;
    private String mode;
}
