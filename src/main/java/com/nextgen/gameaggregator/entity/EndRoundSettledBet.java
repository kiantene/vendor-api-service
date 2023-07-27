package com.nextgen.gameaggregator.entity;

import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

@Data
@NoArgsConstructor
public class EndRoundSettledBet extends BetInformation {

    private String agentPlayerUsername;
    private String currencyCode;
    private String gameCode;
    private Integer gaResultType;
    private Long endRoundProcessTime;
    private Integer processEndRoundCounter;

    public EndRoundSettledBet(SettledBet settledBet, String agentPlayerUsername, String currencyCode, String gameCode) {

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.map(settledBet, this);

        this.setStatus(BetStatus.SETTLED.code);
        this.agentPlayerUsername = agentPlayerUsername;
        this.currencyCode = currencyCode;
        this.gameCode = gameCode;
        this.gaResultType = settledBet.getResultType();
        this.endRoundProcessTime = System.currentTimeMillis();
        this.processEndRoundCounter = 0;
    }

}
