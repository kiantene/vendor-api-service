package com.nextgen.gameaggregator.game.launcher.lucky365;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameLaunchResponse {

    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data{
        private String loginUrl;
    }
}
