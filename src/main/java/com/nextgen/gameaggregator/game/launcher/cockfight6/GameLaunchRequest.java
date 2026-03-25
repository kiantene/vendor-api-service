package com.nextgen.gameaggregator.game.launcher.cockfight6;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GameLaunchRequest {
    private String agent;
    private String external_player_id;
    private Integer login_device;
    private String language;
}
