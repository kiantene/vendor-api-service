package com.nextgen.gameaggregator.game.launcher.cockfight6;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameLaunchResponse {
    private Integer code;

    private Data data;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String entry;
        private long isNewPlayer;
        private String balance;
    }
}
