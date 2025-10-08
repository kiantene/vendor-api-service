package com.nextgen.gameaggregator.game.launcher.crystal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataResponse {
    @JsonProperty("url")
    private String url;
}


