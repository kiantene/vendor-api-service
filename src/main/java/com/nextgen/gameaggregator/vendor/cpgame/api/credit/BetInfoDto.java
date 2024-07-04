package com.nextgen.gameaggregator.vendor.cpgame.api.credit;

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
    @JsonProperty("win_amount")
    private Double winAmount;

    @NotBlank
    @JsonProperty("round_id")
    private String roundId;

    @NotBlank
    @JsonProperty("settle_type")
    private String settleType;
}
