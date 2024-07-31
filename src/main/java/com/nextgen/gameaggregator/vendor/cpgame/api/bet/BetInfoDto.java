package com.nextgen.gameaggregator.vendor.cpgame.api.bet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.vendor.cpgame.dto.CommonBetInfoDto;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetInfoDto extends CommonBetInfoDto {

    @NotNull
    @JsonProperty("bet_amount")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal betAmount;

    @NotNull
    @JsonProperty("win_amount")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal winAmount;

    @NotNull
    @JsonProperty("transfer_amount")
    @Digits(integer = 20, fraction = 8)
    private BigDecimal transferAmount;

    @JsonProperty("parent_bet_id")
    @Size(max = 255)
    private String parentBetId;

    @JsonProperty("is_settled")
    private Integer isSettled;

    private Integer jackpot;

}
