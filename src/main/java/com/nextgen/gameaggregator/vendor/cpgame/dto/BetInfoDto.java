package com.nextgen.gameaggregator.vendor.cpgame.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetInfoDto {

    @NotBlank
    @JsonProperty("bet_id")
    @Size(max = 255)
    private String betId;

    @JsonProperty("bet_amount")
    private BigDecimal betAmount;

    @JsonProperty("win_amount")
    private BigDecimal winAmount;

    @JsonProperty("transfer_amount")
    private BigDecimal transferAmount;

    @JsonProperty("parent_bet_id")
    @Size(max = 255)
    private String parentBetId;

    @JsonProperty("is_settled")
    private Integer isSettled;

    private Integer jackpot;

    public BigDecimal getWinAmount() {
        if (winAmount.scale() > 8) {
            return winAmount.setScale(8, RoundingMode.DOWN);  // If there are more than 8 decimals, set to 8 decimals
        } else {
            return winAmount;  // If 8 or fewer decimals, return as is
        }
    }

    public BigDecimal getBetAmount() {
        if (betAmount.scale() > 8) {
            return betAmount.setScale(8, RoundingMode.DOWN);  // If there are more than 8 decimals, set to 8 decimals
        } else {
            return betAmount;  // If 8 or fewer decimals, return as is
        }
    }

    public BigDecimal getTransferAmount() {
        if (transferAmount.scale() > 8) {
            return transferAmount.setScale(8, RoundingMode.DOWN);  // If there are more than 8 decimals, set to 8 decimals
        } else {
            return transferAmount;  // If 8 or fewer decimals, return as is
        }
    }

}
