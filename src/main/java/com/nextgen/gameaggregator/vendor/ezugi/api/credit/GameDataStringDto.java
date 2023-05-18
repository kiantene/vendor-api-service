package com.nextgen.gameaggregator.vendor.ezugi.api.credit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDataStringDto {
    @JsonProperty("BetAmount")
    private Double BetAmount;
    @JsonProperty("WinAmount")
    private Double WinAmount;
}
