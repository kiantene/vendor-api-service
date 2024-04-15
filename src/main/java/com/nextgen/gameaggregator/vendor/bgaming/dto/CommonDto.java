package com.nextgen.gameaggregator.vendor.bgaming.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonDto {
    @NotBlank
    @JsonProperty("user_id")
    private String userId;
    @NotBlank
    @JsonProperty("currency")
    private String currency;
    @NotBlank
    @JsonProperty("game")
    private String game;
    @JsonProperty("game_id")
    private String vendorRoundId;
    @JsonProperty("finished")
    private Boolean finished = false;
    @JsonProperty("actions")
    private List<ActionDto> actions;
    @JsonIgnore
    private ActionDto actionDto;
    /*
    @JsonIgnore
    private Boolean isSettled;
    */
}
