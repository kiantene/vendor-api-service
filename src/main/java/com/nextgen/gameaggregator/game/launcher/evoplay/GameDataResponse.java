package com.nextgen.gameaggregator.game.launcher.evoplay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataResponse {
    @JsonProperty("link")
    private String link;
}
