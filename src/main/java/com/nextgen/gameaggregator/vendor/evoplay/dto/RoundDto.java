package com.nextgen.gameaggregator.vendor.evoplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoundDto {
    private String game_bet;
}
