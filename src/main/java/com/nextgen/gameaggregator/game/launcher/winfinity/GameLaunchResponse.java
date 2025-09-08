package com.nextgen.gameaggregator.game.launcher.winfinity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GameLaunchResponse {
    private GameUrlDataVo data;

    @Data
    public static class GameUrlDataVo {
        @NotBlank
        private String frameUrl;
        private String masterSessionId;
    }
}