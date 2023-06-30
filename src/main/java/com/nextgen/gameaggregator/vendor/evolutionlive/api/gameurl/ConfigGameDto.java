package com.nextgen.gameaggregator.vendor.evolutionlive.api.gameurl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigGameDto {
    private String category; // (Optional) Lobby login and go to specific category
    @JsonProperty("interface")
    private String game_interface; // (Optional) Login game graphic interface
    private GameTableDto table;
}
