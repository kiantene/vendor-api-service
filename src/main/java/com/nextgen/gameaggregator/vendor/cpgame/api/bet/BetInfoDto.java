package com.nextgen.gameaggregator.vendor.cpgame.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetInfoDto {

    @NotBlank
    @JsonProperty("bet_id")
    @Pattern(regexp = "^[^\\u4E00-\\u9FFF]*$")
    private String betId;

    @NotNull
    @JsonProperty("bet_amount")
    private Double betAmount;

    @NotNull
    @JsonProperty("win_amount")
    private Double winAmount;

    @NotNull
    @JsonProperty("transfer_amount")
    private Double transferAmount;

    @NotNull
    @JsonProperty("bet_type")
    private Integer betType;

    @JsonProperty("round_id")
    private String roundId;

    @JsonProperty("act_info")
    private String actInfo;

    private String jackpot;
}
