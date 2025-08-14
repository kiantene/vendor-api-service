package com.nextgen.gameaggregator.game.launcher.crystal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GameLaunchResponse {
    @NotNull(message = "Data can not be blank")
    @JsonProperty("data")
    private GameDataReponse data;
}
