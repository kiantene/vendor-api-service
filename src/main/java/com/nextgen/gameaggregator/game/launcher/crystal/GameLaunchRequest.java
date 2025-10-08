package com.nextgen.gameaggregator.game.launcher.crystal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameLaunchRequest {
    private String gameCode;
    private String brandCode;
    private String currencyCode;
    private String playerId;
}
