package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

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

    public BetHistoryUncap(BetHistoryV3 betHistoryV3, SettledBet settledBet) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(betHistoryV3, this);

        this.uncapWinAmount = settledBet.getUncapWinAmount();
        this.uncapWinLoss = settledBet.getUncapWinLoss();
        this.uncapJackpotAmount = settledBet.getUncapJackpotAmount();
        this.uncapEffectiveTurnover = settledBet.getUncapEffectiveTurnover();

    }
}
