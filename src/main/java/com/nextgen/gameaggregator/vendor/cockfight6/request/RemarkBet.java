package com.nextgen.gameaggregator.vendor.cockfight6.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemarkBet {
    @NotBlank
    @JsonProperty("game_round_id")
    private String gameRoundId;
}
