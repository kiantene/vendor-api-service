package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BetHistoryUncap extends BetHistoryV3 {

    @JsonProperty("uncap_win_amount")
    private BigDecimal uncapWinAmount;

    @JsonProperty("uncap_win_loss")
    private BigDecimal uncapWinLoss;

    @JsonProperty("uncap_jackpot_amount")
    private BigDecimal uncapJackpotAmount;

    @JsonProperty("uncap_effective_turnover")
    private BigDecimal uncapEffectiveTurnover;
}
