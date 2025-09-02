package com.nextgen.gameaggregator.game.launcher.crystal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataReponse {
    @JsonProperty("url")
    @NotBlank
    @NotNull
    private String url;
}


