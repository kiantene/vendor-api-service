package com.nextgen.gameaggregator.game.launcher.saba;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GameLaunchResponse {
    private Integer errorCode;
    private String message;
    @NotNull(message = "Data can not be blank")
    private String data;
}
