package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BetResultDlq {
    private Integer vendorId;
    private Long vendorPlayerId;
    private Integer agentId;
    private Integer vendorGameId;

    // Fields from BetResultData
    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private String gameId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private BigDecimal jackpotAmount;
    private Long vendorBetTime;
    private Long resultTime;
    private Long vendorSettleTime;
    private Integer isFreespin;
    private BetStatus betStatus;
    private Long requestTime;

    public BetResultDlq(BetResultData betResultData) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(betResultData, this);
    }
}
